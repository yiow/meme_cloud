"""
模仿大赛 WebSocket 管理器 — 2人实时匹配 + 游戏同步

协议（JSON 文本帧）：

客户端 → 服务端:
  {"type": "join_queue"}
  {"type": "leave_queue"}

服务端 → 客户端:
  {"type": "waiting", "msg": "等待对手加入…"}
  {"type": "match_found", "match_id": N, "target": {...}, "opponent": "玩家X", "countdown": 5}
  {"type": "opponent_ready", "msg": "对手已完成模仿"}
  {"type": "result", "my_score": 85, "opponent_score": 72, "winner": "you"|"opponent"|"draw"}
  {"type": "error", "msg": "…"}
"""

import json
import time
from typing import Any

from app.services import game_service

import asyncio

# ── 内存状态 ──
_main_loop: asyncio.AbstractEventLoop | None = None
_ws_connections: dict[int, Any] = {}      # user_id → WebSocket
_waiting_queue: list[int] = []             # 排队 user_id
_active_matches: dict[int, dict] = {}      # match_id → match state
_next_match_id = 1000


def set_main_loop(loop: asyncio.AbstractEventLoop):
    global _main_loop
    _main_loop = loop


def _new_match_id() -> int:
    global _next_match_id
    _next_match_id += 1
    return _next_match_id


async def handle_websocket(websocket, user_id: int):
    _ws_connections[user_id] = websocket
    try:
        while True:
            raw = await websocket.receive_text()
            msg = json.loads(raw)
            msg_type = msg.get("type", "")
            if msg_type == "join_queue":
                await _handle_join_queue(user_id)
            elif msg_type == "leave_queue":
                _handle_leave_queue(user_id)
    except Exception:
        pass
    finally:
        _handle_disconnect(user_id)


# ── 排队 ──

async def _handle_join_queue(user_id: int):
    for match_id, match in _active_matches.items():
        if user_id in match.get("players", []):
            ws = _ws_connections.get(user_id)
            if ws:
                await ws.send_text(json.dumps({"type": "error", "msg": "你已在对局中"}))
            return

    if user_id in _waiting_queue:
        return

    _waiting_queue.append(user_id)

    if len(_waiting_queue) >= 2:
        p1 = _waiting_queue.pop(0)
        p2 = _waiting_queue.pop(0)
        await _create_match(p1, p2)
    else:
        ws = _ws_connections.get(user_id)
        if ws:
            await ws.send_text(json.dumps({"type": "waiting", "msg": "等待对手加入…"}))


def _handle_leave_queue(user_id: int):
    if user_id in _waiting_queue:
        _waiting_queue.remove(user_id)


def _handle_disconnect(user_id: int):
    _handle_leave_queue(user_id)
    _ws_connections.pop(user_id, None)
    for match_id, match in list(_active_matches.items()):
        if user_id in match.get("players", []):
            opponent_id = match["players"][0] if match["players"][1] == user_id else match["players"][1]
            opponent_ws = _ws_connections.get(opponent_id)
            if opponent_ws:
                import asyncio
                asyncio.create_task(opponent_ws.send_text(json.dumps(
                    {"type": "opponent_left", "msg": "对手已离开"}
                )))
            del _active_matches[match_id]
            break


# ── 创建对局 ──

async def _create_match(p1: int, p2: int):
    match_id = _new_match_id()
    target = game_service.get_random_target()
    match = {
        "id": match_id,
        "players": [p1, p2],
        "target": target,
        "scores": {},        # user_id → score
        "countdown": 5,
        "created_at": time.time(),
    }
    _active_matches[match_id] = match

    for pid in [p1, p2]:
        ws = _ws_connections.get(pid)
        if ws:
            other = p2 if pid == p1 else p1
            await ws.send_text(json.dumps({
                "type": "match_found",
                "match_id": match_id,
                "target": target,
                "opponent": f"玩家{other}",
                "countdown": 5,
            }))


def submit_score_sync(user_id: int, match_id: int, score: int):
    """同步版本 — 从 HTTP 端点调用（端点运行在线程池中无 event loop）"""
    if _main_loop is None:
        return
    asyncio.run_coroutine_threadsafe(
        _submit_score_async(user_id, match_id, score), _main_loop
    )


# ── 提交得分（由 HTTP camera-submit 调用）──

async def _submit_score_async(user_id: int, match_id: int, score: int):
    """玩家提交得分。双方都提交后广播最终结果。"""
    match = _active_matches.get(match_id)
    if not match:
        return

    match["scores"][user_id] = score

    # 通知对手
    for pid in match["players"]:
        if pid != user_id:
            ws = _ws_connections.get(pid)
            if ws:
                try:
                    await ws.send_text(json.dumps({
                        "type": "opponent_ready",
                        "msg": "对手已完成模仿"
                    }))
                except Exception:
                    pass

    # 双方都提交了 → 计算最终结果并广播
    if match["scores"].keys() >= set(match["players"]):
        results = _build_results(match)
        for pid in match["players"]:
            ws = _ws_connections.get(pid)
            if ws:
                try:
                    own = match["scores"].get(pid, 0)
                    other_id = match["players"][0] if match["players"][1] == pid else match["players"][1]
                    other = match["scores"].get(other_id, 0)
                    winner = "draw" if own == other else ("you" if own > other else "opponent")
                    await ws.send_text(json.dumps({
                        "type": "result",
                        "my_score": own,
                        "opponent_score": other,
                        "opponent_name": f"玩家{other_id}",
                        "winner": winner,
                    }))
                except Exception:
                    pass
        del _active_matches[match_id]


def _build_results(match: dict) -> list[dict]:
    results = []
    for pid in match["players"]:
        results.append({
            "user_id": pid,
            "nickname": f"玩家{pid}",
            "score": match["scores"].get(pid, 0),
        })
    results.sort(key=lambda x: x["score"], reverse=True)
    for i, r in enumerate(results):
        r["rank"] = i + 1
    return results
