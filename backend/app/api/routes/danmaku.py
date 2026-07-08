"""Danmaku Battle Room - API Routes"""
import hashlib
import logging
import os as _os
from pathlib import Path
from fastapi import APIRouter, Depends, Header, Query, UploadFile, File, Form, WebSocket, WebSocketDisconnect
from sqlalchemy import select, or_
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.core.security import decode_access_token
from app.models.danmaku_room import DanmakuRoom
from app.models.emoji import Emoji
from app.models.favorite import Favorite
from app.models.user import User
from app.schemas.danmaku import (
    CreateRoomRequest,
    RoomBrief,
    RoomListResponse,
)
from app.schemas.user import ApiResponse
from app.services.danmaku_service import room_manager
logger = logging.getLogger("danmaku")
router = APIRouter(prefix="/api/danmaku", tags=["danmaku"])
MEME_IMG_DIR = Path(__file__).resolve().parent.parent.parent.parent.parent / "meme_match" / "memes"
# -- Auth helpers --

def _get_token_from_header(authorization="") -> str | None:
    if authorization.startswith("Bearer "):
        return authorization[7:]
    return None

def _get_current_user_id(token=None):
    if not token:
        return None
    from app.core.security import decode_access_token
    payload = decode_access_token(token)
    if payload is None:
        return None
    return int(payload.get("sub", 0))


# REST: Room management


@router.post("/rooms", response_model=ApiResponse)
def create_room(
    req: CreateRoomRequest,
    authorization: str = Header(""),
    db: Session = Depends(get_db),
):
    token = _get_token_from_header(authorization)
    user_id = _get_current_user_id(token)
    if not user_id:
        return ApiResponse(code=401, msg="login required")
    user_info = _get_user_info(db, user_id)
    if not user_info:
        return ApiResponse(code=404, msg="user not found")
    room = DanmakuRoom(name=req.name, creator_id=user_id, max_users=20, status=1)
    db.add(room)
    db.commit()
    db.refresh(room)
    brief = _build_room_brief(room, db)
    return ApiResponse(msg="room created", data=brief.model_dump())


@router.get("/rooms", response_model=ApiResponse)
def list_rooms(
    keyword: str = Query(default=""),
    authorization: str = Header(""),
    db: Session = Depends(get_db),
):
    query = select(DanmakuRoom).where(DanmakuRoom.status == 1)
    if keyword:
        query = query.where(DanmakuRoom.name.like(f"%{keyword}%"))
    query = query.order_by(DanmakuRoom.last_active_at.desc())
    db_rooms = db.execute(query).scalars().all()
    briefs = []
    for r in db_rooms:
        brief = _build_room_brief(r, db)
        briefs.append(brief)
    return ApiResponse(msg="ok", data=RoomListResponse(rooms=briefs, total=len(briefs)).model_dump())


@router.get("/rooms/{room_id}", response_model=ApiResponse)
def get_room(room_id: int, authorization: str = Header(''), db: Session = Depends(get_db)):
    db_room = db.get(DanmakuRoom, room_id)
    if not db_room or db_room.status == 0:
        return ApiResponse(code=404, msg="room not found")
    brief = _build_room_brief(db_room, db)
    return ApiResponse(msg="ok", data=brief.model_dump())


@router.post("/rooms/random", response_model=ApiResponse)
async def random_room(authorization: str = Header(''), db: Session = Depends(get_db)):
    token = _get_token_from_header(authorization)
    user_id = _get_current_user_id(token)
    if not user_id:
        return ApiResponse(code=401, msg="login required")
    room = await room_manager.get_random_active_room()
    if not room:
        return ApiResponse(code=404, msg="no rooms available")
    db_room = db.get(DanmakuRoom, room.id)
    if not db_room:
        return ApiResponse(code=404, msg="room not found")
    brief = _build_room_brief(db_room, db)
    brief.user_count = room.user_count
    return ApiResponse(msg="ok", data={**brief.model_dump(), "token": token})


@router.get("/emojis/personal", response_model=ApiResponse)
def personal_emojis(authorization: str = Header(''), db: Session = Depends(get_db)):
    token = _get_token_from_header(authorization)
    user_id = _get_current_user_id(token)
    if not user_id:
        return ApiResponse(code=401, msg="login required")
    fav_stmt = select(Emoji).join(Favorite, Favorite.emoji_id == Emoji.id).where(Favorite.user_id == user_id, Emoji.status == 1).order_by(Favorite.created_at.desc())
    fav_emojis = db.execute(fav_stmt).scalars().all()
    fav_ids = {e.id for e in fav_emojis}
    own_stmt = select(Emoji).where(Emoji.uploader_id == user_id, Emoji.status == 1).order_by(Emoji.created_at.desc())
    own_emojis = [e for e in db.execute(own_stmt).scalars().all() if e.id not in fav_ids]
    all_emojis = list(fav_emojis) + own_emojis
    result = [{'id': e.id, 'file_url': e.file_url, 'thumbnail_url': e.thumbnail_url, 'description': e.description} for e in all_emojis]
    return ApiResponse(msg="ok", data=result)


@router.get("/emojis/search", response_model=ApiResponse)
def search_emojis(q: str = Query(default='', alias='q'), authorization: str = Header(''), db: Session = Depends(get_db)):
    if not q:
        stmt = select(Emoji).where(Emoji.status == 1).order_by(Emoji.like_count.desc(), Emoji.created_at.desc()).limit(30)
    else:
        stmt = select(Emoji).where(Emoji.status == 1, or_(Emoji.description.like(f'%{q}%'), Emoji.file_url.like(f'%{q}%'))).order_by(Emoji.like_count.desc()).limit(30)
    emojis = db.execute(stmt).scalars().all()
    result = [{'id': e.id, 'file_url': e.file_url, 'thumbnail_url': e.thumbnail_url, 'description': e.description} for e in emojis]
    return ApiResponse(msg="ok", data=result)


@router.post("/emojis/upload", response_model=ApiResponse)
async def upload_emoji(file: UploadFile = File(...), description: str = Form(default=''), authorization: str = Header(''), db: Session = Depends(get_db)):
    token = _get_token_from_header(authorization)
    user_id = _get_current_user_id(token)
    if not user_id:
        return ApiResponse(code=401, msg="login required")
    content = await file.read()
    md5 = hashlib.md5(content).hexdigest()
    existing = db.execute(select(Emoji).where(Emoji.file_md5 == md5)).scalar_one_or_none()
    if existing:
        return ApiResponse(msg="already exists", data={"id": existing.id, "file_url": existing.file_url})
    ext = _os.path.splitext(file.filename or 'image.png')[1].lower()
    if ext not in ('.jpg', '.jpeg', '.png', '.gif', '.webp'): ext = '.png'
    filename = md5 + ext
    filepath = MEME_IMG_DIR / filename
    MEME_IMG_DIR.mkdir(parents=True, exist_ok=True)
    with open(filepath, 'wb') as f: f.write(content)
    file_url = '/static/memes/' + filename
    emoji = Emoji(file_url=file_url, thumbnail_url=file_url, file_md5=md5, uploader_id=user_id, description=description or filename, source_type=0, status=1)
    db.add(emoji); db.commit(); db.refresh(emoji)
    return ApiResponse(msg='upload success', data={'id': emoji.id, 'file_url': emoji.file_url, 'thumbnail_url': emoji.thumbnail_url})


@router.websocket("/ws/{room_id}")
async def websocket_endpoint(websocket: WebSocket, room_id: int, token: str = ''):
    payload = decode_access_token(token)
    if payload is None:
        await websocket.close(code=4001, reason="auth failed")
        return
    user_id = int(payload.get('sub', 0))
    db = next(get_db())
    try:
        user = db.get(User, user_id)
    finally:
        db.close()
    if not user:
        await websocket.close(code=4001, reason="user not found")
        return
    await websocket.accept()
    user_info = {'id': user.id, 'username': user.username, 'nickname': user.nickname, 'avatar_url': user.avatar_url}
    try:
        join_result = await room_manager.join_room(room_id=room_id, ws=websocket, user_id=user.id, username=user.username, nickname=user.nickname, avatar_url=user.avatar_url)
    except Exception as e:
        await websocket.close(code=1011)
        return
    if 'error' in join_result:
        await websocket.send_json({'type': 'error', 'data': {'message': join_result['error']}})
        await websocket.close(code=1000)
        return
    await websocket.send_json({'type': 'room_info', 'data': {'room': join_result['room'], 'is_spectator': join_result['is_spectator']}})
    await websocket.send_json({'type': 'history', 'data': {'messages': join_result['history']}})
    await websocket.send_json({'type': 'user_list', 'data': {'users': join_result['users']}})
    try:
        while True:
            raw = await websocket.receive_json()
            if raw.get('type') == 'ping':
                await websocket.send_json({'type': 'pong'})
            elif raw.get('type') == 'send_meme':
                emoji_id = raw.get('emoji_id')
                emoji_url = raw.get('emoji_url', '')
                if not emoji_id:
                    await websocket.send_json({'type': 'error', 'data': {'message': 'missing emoji_id'}})
                    continue
                result = await room_manager.send_meme(room_id=room_id, user_id=user.id, emoji_id=emoji_id, emoji_url=emoji_url)
                if result is None:
                    await websocket.send_json({'type': 'error', 'data': {'message': 'rate limited, 1 per second'}})
                elif 'error' in result:
                    await websocket.send_json({'type': 'error', 'data': {'message': result['error']}})
            else:
                await websocket.send_json({'type': 'error', 'data': {'message': 'unknown type'}})
    except WebSocketDisconnect:
        pass
    except Exception as e:
        pass
    finally:
        await room_manager.leave_room(room_id, user.id)


@router.get("/emojis/all", response_model=ApiResponse)
def all_emojis(authorization: str = Header(''), db: Session = Depends(get_db)):
    stmt = select(Emoji).where(Emoji.status == 1).order_by(Emoji.like_count.desc()).limit(50)
    emojis = db.execute(stmt).scalars().all()
    result = [{'id': e.id, 'file_url': e.file_url, 'thumbnail_url': e.thumbnail_url, 'description': e.description} for e in emojis]
    return ApiResponse(msg="ok", data=result)