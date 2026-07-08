"""弹幕斗图室 Pydantic schemas"""

from datetime import datetime
from typing import Any

from pydantic import BaseModel, Field


# ── Request ──

class CreateRoomRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=50, examples=["今晚吃鸡表情包大战"])


class SearchRoomRequest(BaseModel):
    keyword: str = Field(default="", examples=["吃鸡"])


# ── Response ──

class RoomBrief(BaseModel):
    id: int
    name: str
    creator_id: int
    creator_name: str | None = None
    user_count: int = 0
    max_users: int = 20
    status: int = 1
    created_at: datetime | None = None
    last_active_at: datetime | None = None


class RoomJoinInfo(BaseModel):
    """进入房间时返回的信息"""
    room: RoomBrief
    token: str


class UserInfo(BaseModel):
    id: int
    nickname: str | None = None
    username: str
    avatar_url: str | None = None


class DanmakuMessage(BaseModel):
    """弹幕消息"""
    type: str  # "meme" | "system"
    user: UserInfo | None = None
    emoji_id: int | None = None
    emoji_url: str | None = None
    text: str | None = None
    timestamp: float = 0


class EmojiBrief(BaseModel):
    id: int
    file_url: str
    thumbnail_url: str | None = None
    description: str | None = None


class RoomListResponse(BaseModel):
    rooms: list[RoomBrief]
    total: int = 0


# ── WebSocket messages ──

class WSClientMessage(BaseModel):
    """客户端发送的WebSocket消息"""
    type: str = Field(..., pattern="^(send_meme|ping)$")
    emoji_id: int | None = None


class WSServerMessage(BaseModel):
    """服务端发送的WebSocket消息"""
    type: str  # "meme" | "system" | "user_list" | "history" | "pong" | "error"
    data: Any = None
