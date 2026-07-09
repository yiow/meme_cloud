"""
弹幕斗图室 — 核心服务

RoomManager: 内存级房间管理
  - 每个房间最多20人（超出的旁观）
  - 单用户每秒最多1条
  - 创建后1小时无活动自动关闭
  - 不保留历史，进入时仅加载最近10条
"""

import asyncio
import logging
import time
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Any

from fastapi import WebSocket

logger = logging.getLogger("danmaku")

# ── Constants ──
MAX_USERS = 20
RATE_LIMIT_SEC = 1.0
INACTIVE_TIMEOUT_SEC = 3600  # 1 hour
HISTORY_LIMIT = 10


@dataclass
class RoomUser:
    """房间内用户连接"""
    ws: WebSocket
    user_id: int
    username: str
    nickname: str | None
    avatar_url: str | None
    is_spectator: bool = False  # True = 旁观模式
    _last_send_time: float = 0.0

    def can_send(self) -> bool:
        now = time.time()
        if now - self._last_send_time >= RATE_LIMIT_SEC:
            self._last_send_time = now
            return True
        return False


@dataclass
class MemeMessage:
    """单条弹幕消息"""
    user_id: int
    username: str
    nickname: str | None
    avatar_url: str | None
    meme_id: str
    meme_url: str
    timestamp: float = field(default_factory=time.time)


@dataclass
class Room:
    """房间状态"""
    id: int
    name: str
    creator_id: int
    max_users: int = MAX_USERS
    status: int = 1  # 0=closed, 1=active
    created_at: float = field(default_factory=time.time)
    last_active_at: float = field(default_factory=time.time)
    users: dict[int, RoomUser] = field(default_factory=dict)  # user_id -> RoomUser
    history: list[MemeMessage] = field(default_factory=list)   # 最近10条

    @property
    def user_count(self) -> int:
        return len(self.users)

    @property
    def active_count(self) -> int:
        """非旁观的活跃用户数"""
        return sum(1 for u in self.users.values() if not u.is_spectator)

    def add_history(self, msg: MemeMessage):
        self.history.append(msg)
        if len(self.history) > HISTORY_LIMIT:
            self.history = self.history[-HISTORY_LIMIT:]

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "name": self.name,
            "creator_id": self.creator_id,
            "user_count": self.user_count,
            "max_users": self.max_users,
            "status": self.status,
            "created_at": self.created_at,
            "last_active_at": self.last_active_at,
        }


class RoomManager:
    """全局房间管理器"""

    def __init__(self):
        self._rooms: dict[int, Room] = {}
        self._lock = asyncio.Lock()
        self._cleanup_task: asyncio.Task | None = None
        # DB session factory for persistence
        self._db_session_factory = None

    def set_db_session_factory(self, factory):
        self._db_session_factory = factory

    # ── Room CRUD ──

    async def create_room(self, room_id: int, name: str, creator_id: int) -> Room:
        async with self._lock:
            room = Room(
                id=room_id,
                name=name,
                creator_id=creator_id,
            )
            self._rooms[room_id] = room
            self._start_cleanup_if_needed()
            return room

    async def get_room(self, room_id: int) -> Room | None:
        async with self._lock:
            return self._rooms.get(room_id)

    async def get_rooms(self, keyword: str = "") -> list[Room]:
        async with self._lock:
            rooms = list(self._rooms.values())

        if keyword:
            kw = keyword.lower()
            rooms = [r for r in rooms if kw in r.name.lower()]

        # 只返回活跃房间
        rooms = [r for r in rooms if r.status == 1]
        rooms.sort(key=lambda r: r.last_active_at, reverse=True)
        return rooms

    async def get_random_active_room(self) -> Room | None:
        """随机进入一个活跃房间"""
        async with self._lock:
            active = [r for r in self._rooms.values() if r.status == 1 and r.active_count < r.max_users]
            if not active:
                return None
            import random
            return random.choice(active)

    async def close_room(self, room_id: int):
        async with self._lock:
            room = self._rooms.get(room_id)
            if room:
                room.status = 0
                # Notify all users
                await self._broadcast_json(room, {
                    "type": "system",
                    "data": {"message": "房间已关闭（长时间无人活动）"},
                })
                # Close all connections
                for ru in list(room.users.values()):
                    try:
                        await ru.ws.close(code=1000, reason="房间已关闭")
                    except Exception:
                        pass
                self._rooms.pop(room_id, None)

    # ── User management ──

    async def join_room(
        self,
        room_id: int,
        ws: WebSocket,
        user_id: int,
        username: str,
        nickname: str | None,
        avatar_url: str | None,
    ) -> dict:
        """加入房间，返回初始数据"""
        async with self._lock:
            room = self._rooms.get(room_id)
            if not room or room.status == 0:
                return {"error": "房间不存在或已关闭"}

            # 检查是否已在房间
            existing = room.users.get(user_id)
            if existing:
                # 更新WebSocket连接
                existing.ws = ws
                ru = existing
            else:
                # 判断是否旁观
                is_spectator = room.active_count >= room.max_users
                ru = RoomUser(
                    ws=ws,
                    user_id=user_id,
                    username=username,
                    nickname=nickname,
                    avatar_url=avatar_url,
                    is_spectator=is_spectator,
                )
                room.users[user_id] = ru

            room.last_active_at = time.time()

        # 广播加入消息
        display_name = ru.nickname or ru.username
        if ru.is_spectator:
            system_msg = f"{display_name} 进入旁观"
        else:
            system_msg = f"{display_name} 加入了斗图"

        await self._broadcast_json(room, {
            "type": "system",
            "data": {"message": system_msg},
        })

        # 发送用户列表
        await self._broadcast_user_list(room)

        # 返回历史消息
        history = []
        for m in room.history:
            history.append({
                "type": "meme",
                "user": {
                    "id": m.user_id,
                    "username": m.username,
                    "nickname": m.nickname,
                    "avatar_url": m.avatar_url,
                },
                "meme_id": m.meme_id,
                "meme_url": m.meme_url,
                "timestamp": m.timestamp,
            })

        return {
            "room": room.to_dict(),
            "is_spectator": ru.is_spectator,
            "history": history,
            "users": self._get_user_list(room),
        }

    async def leave_room(self, room_id: int, user_id: int):
        async with self._lock:
            room = self._rooms.get(room_id)
            if not room:
                return

            ru = room.users.pop(user_id, None)
            if not ru:
                return

            room.last_active_at = time.time()
            display_name = ru.nickname or ru.username

        await self._broadcast_json(room, {
            "type": "system",
            "data": {"message": f"{display_name} 离开了斗图"},
        })
        await self._broadcast_user_list(room)

    # ── Meme sending ──

    async def send_meme(
        self,
        room_id: int,
        user_id: int,
        meme_id: str,
        meme_url: str,
    ) -> dict | None:
        """发送表情包弹幕，返回None表示被限流"""
        async with self._lock:
            room = self._rooms.get(room_id)
            if not room:
                return {"error": "房间不存在"}

            ru = room.users.get(user_id)
            if not ru:
                return {"error": "你不在房间中"}

            if ru.is_spectator:
                return {"error": "旁观模式无法发送表情包"}

            if not ru.can_send():
                return None  # 被限流

            msg = MemeMessage(
                user_id=user_id,
                username=ru.username,
                nickname=ru.nickname,
                avatar_url=ru.avatar_url,
                meme_id=meme_id,
                meme_url=meme_url,
            )
            room.add_history(msg)
            room.last_active_at = time.time()

        payload = {
            "type": "meme",
            "data": {
                "user": {
                    "id": ru.user_id,
                    "username": ru.username,
                    "nickname": ru.nickname,
                    "avatar_url": ru.avatar_url,
                },
                "meme_id": meme_id,
                "meme_url": meme_url,
                "timestamp": msg.timestamp,
            },
        }

        # 广播给所有人（包括发送者自己）
        await self._broadcast_json(room, payload)
        return {}

    # ── Broadcast helpers ──

    async def _broadcast_json(self, room: Room, data: dict, exclude_user_id: int | None = None):
        """广播JSON消息给房间所有用户"""
        dead: list[int] = []
        for uid, ru in room.users.items():
            if ru is exclude_user_id:
                continue
            try:
                await ru.ws.send_json(data)
            except Exception:
                dead.append(uid)

        if dead:
            async with self._lock:
                for uid in dead:
                    room.users.pop(uid, None)

    async def _broadcast_user_list(self, room: Room):
        users = self._get_user_list(room)
        await self._broadcast_json(room, {
            "type": "user_list",
            "data": {"users": users},
        })

    def _get_user_list(self, room: Room) -> list[dict]:
        return [
            {
                "id": ru.user_id,
                "username": ru.username,
                "nickname": ru.nickname,
                "avatar_url": ru.avatar_url,
                "is_spectator": ru.is_spectator,
            }
            for ru in room.users.values()
        ]

    # ── Cleanup task ──

    def _start_cleanup_if_needed(self):
        if self._cleanup_task is None or self._cleanup_task.done():
            self._cleanup_task = asyncio.create_task(self._cleanup_loop())

    async def _cleanup_loop(self):
        """每分钟检查一次，关闭超时房间"""
        while True:
            await asyncio.sleep(60)
            now = time.time()
            to_close: list[int] = []

            async with self._lock:
                for rid, room in list(self._rooms.items()):
                    if room.status == 0:
                        continue
                    if now - room.last_active_at > INACTIVE_TIMEOUT_SEC:
                        to_close.append(rid)

            for rid in to_close:
                logger.info(f"Room {rid} closed due to inactivity")
                await self.close_room(rid)

            # 如果没有房间了，停止清理任务
            async with self._lock:
                if not self._rooms:
                    break


# ── Global singleton ──
room_manager = RoomManager()
