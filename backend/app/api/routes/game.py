"""模仿大赛 API — WebSocket匹配 + 随机目标 / 提交打分 / 排行榜"""

import json

from fastapi import APIRouter, File, Form, UploadFile, WebSocket, WebSocketDisconnect

from app.core.database import SessionLocal
from app.models.game import GameMatch, GameParticipant
from app.schemas.user import ApiResponse
from app.services import game_service, game_ws_manager, mediapipe_extractor, webcam_service

router = APIRouter(prefix="/api/game", tags=["模仿大赛"])

# ── WebSocket 匹配端点已移至 main.py（避免 router 层 403）──

# ── REST 接口 ────────────────────────────────────────────

@router.get("/random-target")
def random_target():
    """随机抽取一个模仿目标表情包"""
    target = game_service.get_random_target()
    return ApiResponse(msg="ok", data=target)


@router.post("/submit")
async def submit_photo(
    file: UploadFile = File(...),
    target_label: str = Form(...),
    target_emoji_id: str = Form(...),
    target_image: str = Form(...),
    user_id: int = Form(1),
    match_id: int = Form(0),
):
    """
    手机拍照上传 + AI 打分。
    如果是多人对局 (match_id > 0)，结果会通过 WebSocket 广播给双方。
    """
    # 1. 读取上传的照片
    image_bytes = await file.read()

    # 2. 提取特征
    features = mediapipe_extractor.extract_gesture_features(image_bytes)
    if features is None:
        return ApiResponse(code=1, msg="未检测到人体姿态，请后退让上半身入镜")

    # 3. AI 打分
    score_result = game_service.score_imitation(features, target_label)

    # 4. 保存记录
    db = SessionLocal()
    try:
        match = GameMatch(
            target_emoji_id=target_emoji_id,
            target_label=target_label,
            target_image=target_image,
            status=2,
        )
        db.add(match)
        db.flush()
        db_match_id = match.id

        participant = GameParticipant(
            match_id=db_match_id,
            user_id=1,  # DB FK: 默认关联 testuser，真实 user_id 仅用于 WebSocket
            score=score_result["score"],
            feature_json=json.dumps(features),
            photo_label=score_result["matched_label"],
        )
        db.add(participant)
        db.commit()
    finally:
        db.close()

    user_result = {
        "user_id": user_id,
        "nickname": f"玩家{user_id}",
        "score": score_result["score"],
        "photo_url": None,
        "rank": 0,
    }

    # 5. 多人对局：通知 WebSocket 管理器（async 直接 await）
    if match_id > 0:
        await game_ws_manager.submit_score(user_id, match_id, score_result["score"])

    return ApiResponse(msg="ok", data={
        "match_id": db_match_id,
        "score": score_result["score"],
        "matched_label": score_result["matched_label"],
        "is_exact_match": score_result["is_exact_match"],
        "avg_distance": score_result.get("avg_distance", 0),
        "user_result": user_result,
    })


@router.post("/camera-submit")
def camera_submit(
    target_label: str = Form(...),
    target_emoji_id: str = Form(...),
    target_image: str = Form(...),
    user_id: int = Form(1),
    match_id: int = Form(0),
):
    """
    PC 摄像头拍照 + AI 打分。
    如果是多人对局 (match_id > 0)，结果会通过 WebSocket 广播给双方。
    """
    # 1. 抓帧
    frame = webcam_service.get_latest_frame()
    if frame is None:
        return ApiResponse(code=1, msg="摄像头未就绪")

    # 2. 提取特征
    features = mediapipe_extractor.extract_gesture_features(frame)
    if features is None:
        return ApiResponse(code=1, msg="未检测到人体姿态，请后退让上半身入镜")

    # 3. AI 打分
    score_result = game_service.score_imitation(features, target_label)

    # 4. 保存记录
    db = SessionLocal()
    try:
        match = GameMatch(
            target_emoji_id=target_emoji_id,
            target_label=target_label,
            target_image=target_image,
            status=2,
        )
        db.add(match)
        db.flush()
        db_match_id = match.id

        participant = GameParticipant(
            match_id=db_match_id,
            user_id=user_id,
            score=score_result["score"],
            feature_json=json.dumps(features),
            photo_label=score_result["matched_label"],
        )
        db.add(participant)
        db.commit()
    finally:
        db.close()

    user_result = {
        "user_id": user_id,
        "nickname": f"玩家{user_id}",
        "score": score_result["score"],
        "photo_url": None,
        "rank": 0,
    }

    # 5. 多人对局：通知 WebSocket 管理器（同步安全）
    if match_id > 0:
        game_ws_manager.submit_score_sync(user_id, match_id, score_result["score"])

    return ApiResponse(msg="ok", data={
        "match_id": db_match_id,
        "score": score_result["score"],
        "matched_label": score_result["matched_label"],
        "is_exact_match": score_result["is_exact_match"],
        "avg_distance": score_result.get("avg_distance", 0),
        "user_result": user_result,
    })


@router.get("/match/{match_id}")
def get_match_result(match_id: int):
    """查看对局详情"""
    db = SessionLocal()
    try:
        match = db.query(GameMatch).filter(GameMatch.id == match_id).first()
        if not match:
            return ApiResponse(code=1, msg="对局不存在")

        participants = db.query(GameParticipant).filter(
            GameParticipant.match_id == match_id
        ).order_by(GameParticipant.score.desc()).all()

        results = []
        for i, p in enumerate(participants):
            results.append({
                "user_id": p.user_id,
                "nickname": f"用户{p.user_id}",
                "score": p.score,
                "photo_url": p.photo_url,
                "rank": i + 1,
            })

        return ApiResponse(msg="ok", data={
            "match_id": match.id,
            "target_label": match.target_label,
            "target_image": match.target_image,
            "results": results,
        })
    finally:
        db.close()


@router.get("/leaderboard")
def leaderboard(limit: int = 20):
    """模仿大赛排行榜"""
    data = game_service.get_leaderboard(limit)
    return ApiResponse(msg="ok", data=data)


@router.get("/history")
def my_history(user_id: int = 1):
    """我的对战历史"""
    db = SessionLocal()
    try:
        matches = db.query(GameParticipant).filter(
            GameParticipant.user_id == user_id
        ).order_by(GameParticipant.created_at.desc()).limit(20).all()

        result = []
        for p in matches:
            match = db.query(GameMatch).filter(GameMatch.id == p.match_id).first()
            result.append({
                "match_id": p.match_id,
                "target_label": match.target_label if match else "",
                "target_image": match.target_image if match else "",
                "score": p.score,
                "photo_label": p.photo_label,
                "created_at": str(p.created_at),
            })

        return ApiResponse(msg="ok", data=result)
    finally:
        db.close()
