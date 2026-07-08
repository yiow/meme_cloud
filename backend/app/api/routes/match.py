"""匹配路由 — POST /api/match/gesture  /api/match/expression  /api/match/record  /api/match/photo"""

import time as _time
from fastapi import APIRouter, File, Form, UploadFile
from fastapi.responses import Response, StreamingResponse

from app.schemas.match import LabelItem, MatchRequest, MatchResponse, RecordRequest
from app.schemas.user import ApiResponse
from app.services import matcher, mediapipe_extractor, webcam_service

router = APIRouter(prefix="/api/match", tags=["匹配"])


# ── PC 摄像头直连（绕过 AVD 虚拟摄像头）──

@router.get("/camera/stream")
def camera_stream():
    """MJPEG 视频流 — 连续推送 JPEG 帧，Android 端丝滑预览无频闪"""
    def generate():
        while True:
            frame = webcam_service.get_latest_frame()
            if frame is not None:
                yield (b'--frame\r\n'
                       b'Content-Type: image/jpeg\r\n\r\n' + frame + b'\r\n')
            _time.sleep(0.033)  # ~30 fps
    return StreamingResponse(
        generate(),
        media_type="multipart/x-mixed-replace; boundary=frame",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.get("/camera/preview")
def camera_preview():
    """返回 PC 端摄像头最新帧 (JPEG) — 单帧快照，用于调试或静态展示"""
    frame = webcam_service.get_latest_frame()
    if frame is None:
        return Response(status_code=503, content=b"webcam not ready")
    return Response(content=frame, media_type="image/jpeg")


@router.get("/camera/status")
def camera_status():
    """查看 PC 摄像头采集状态"""
    return ApiResponse(msg="ok", data=webcam_service.webcam_status())


@router.post("/camera/snapshot")
def camera_snapshot(mode: str = Form("gesture")):
    """用 PC 摄像头当前帧做匹配 — 一步到位：抓帧 → MediaPipe → KNN → 返回表情包"""
    frame = webcam_service.get_latest_frame()
    if frame is None:
        return ApiResponse(code=1, msg="摄像头未就绪，请先 GET /api/match/camera/status")

    features = mediapipe_extractor.extract_gesture_features(frame)
    if features is None:
        return ApiResponse(code=1, msg="未检测到人体姿态，请后退一点让上半身入镜")

    result = matcher.classify(features, mode=mode)
    return ApiResponse(msg="ok", data=result)


@router.post("/gesture", response_model=ApiResponse)
def match_gesture(req: MatchRequest):
    """手势匹配 — 传入 22 维特征向量，返回最匹配的表情包"""
    result = matcher.classify(req.feature, mode="gesture")
    return ApiResponse(msg="ok", data=result)


@router.post("/expression", response_model=ApiResponse)
def match_expression(req: MatchRequest):
    """表情匹配 — 传入 52 维特征向量，返回最匹配的表情包"""
    result = matcher.classify(req.feature, mode="expr")
    return ApiResponse(msg="ok", data=result)


@router.post("/photo")
async def match_photo(file: UploadFile = File(...), mode: str = Form("gesture")):
    """拍照匹配 — 上传照片，后端用 MediaPipe 提取特征 → KNN 匹配 → 返回表情包"""
    image_bytes = await file.read()
    features = mediapipe_extractor.extract_gesture_features(image_bytes)
    if features is None:
        return ApiResponse(code=1, msg="未检测到人体姿态，请拍全身/半身照")
    result = matcher.classify(features, mode=mode)
    return ApiResponse(msg="ok", data=result)


@router.get("/labels", response_model=ApiResponse)
def get_labels(mode: str = "gesture"):
    """获取可匹配的标签列表（手势 or 表情）"""
    labels = matcher.get_labels(mode)
    return ApiResponse(msg="ok", data=labels)


@router.post("/record", response_model=ApiResponse)
def record_sample(req: RecordRequest):
    """录入手势/表情训练样本"""
    matcher.record_sample(req.mode, req.label, req.samples)
    return ApiResponse(msg=f"已录制 {len(req.samples)} 条「{req.label}」样本")
