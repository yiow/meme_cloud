
import sys
import os

fpath = r"C:\Users\ROG\Desktop\meme_test\meme_cloud\backend\app\api\routes\danmaku.py"

# Read existing content
with open(fpath, "r", encoding="utf-8") as f:
    existing = f.read()

# REST endpoints
rest_content = """

# -- REST: Room management --

@router.post("/rooms", response_model=ApiResponse)
def create_room(
    req: CreateRoomRequest,
    authorization: str = Header(""),
    db: Session = Depends(get_db),
):
    \"\"\"??????\"\"\"
    token = _get_token_from_header(authorization)
    user_id = _get_current_user_id(token)
    if not user_id:
        return ApiResponse(code=401, msg="????")

    user_info = _get_user_info(db, user_id)
    if not user_info:
        return ApiResponse(code=404, msg="?????")

    room = DanmakuRoom(
        name=req.name,
        creator_id=user_id,
        max_users=20,
        status=1,
    )
    db.add(room)
    db.commit()
    db.refresh(room)

    brief = _build_room_brief(room, db)
    return ApiResponse(msg="????", data=brief.model_dump())


@router.get("/rooms", response_model=ApiResponse)
def list_rooms(
    keyword: str = Query(default=""),
    authorization: str = Header(""),
    db: Session = Depends(get_db),
):
    \"\"\"??/????\"\"\"
    query = select(DanmakuRoom).where(DanmakuRoom.status == 1)
    if keyword:
        query = query.where(DanmakuRoom.name.like(f"%{keyword}%"))
    query = query.order_by(DanmakuRoom.last_active_at.desc())
    db_rooms = db.execute(query).scalars().all()

    briefs = []
    for r in db_rooms:
        brief = _build_room_brief(r, db)
        # Get live user count from in-memory room manager
        import asyncio
        try:
            loop = asyncio.get_event_loop()
            if loop.is_running():
                coro = room_manager.get_room(r.id)
                fut = asyncio.run_coroutine_threadsafe(coro, loop)
                mgr_room = fut.result(timeout=1)
                if mgr_room:
                    brief.user_count = mgr_room.user_count
        except Exception:
            pass
        briefs.append(brief)

    return ApiResponse(msg="ok", data=RoomListResponse(rooms=briefs, total=len(briefs)).model_dump())


@router.get("/rooms/{room_id}", response_model=ApiResponse)
def get_room(room_id: int, authorization: str = Header(""), db: Session = Depends(get_db)):
    \"\"\"????\"\"\"
    db_room = db.get(DanmakuRoom, room_id)
    if not db_room or db_room.status == 0:
        return ApiResponse(code=404, msg="?????")
    brief = _build_room_brief(db_room, db)
    return ApiResponse(msg="ok", data=brief.model_dump())


@router.post("/rooms/random", response_model=ApiResponse)
async def random_room(authorization: str = Header(""), db: Session = Depends(get_db)):
    \"\"\"??????????\"\"\"
    token = _get_token_from_header(authorization)
    user_id = _get_current_user_id(token)
    if not user_id:
        return ApiResponse(code=401, msg="????")

    room = await room_manager.get_random_active_room()
    if not room:
        return ApiResponse(code=404, msg="???????")

    db_room = db.get(DanmakuRoom, room.id)
    if not db_room:
        return ApiResponse(code=404, msg="?????")

    brief = _build_room_brief(db_room, db)
    brief.user_count = room.user_count
    return ApiResponse(msg="ok", data={**brief.model_dump(), "token": token})
"""

with open(fpath, "a", encoding="utf-8") as f:
    f.write(rest_content)
print("REST endpoints done")
