"""
MediaPipe 特征提取 — PC 后端运行，使用 MediaPipe Tasks API

输出: 22 维特征向量 (10 body + 6 left_hand + 6 right_hand)
于 meme_match/index.html features() + dedosEstado() 完全一致

模型文件: backend/models/pose_landmarker_lite.task + hand_landmarker.task
"""
from math import hypot
from pathlib import Path

import cv2
import mediapipe as mp
import numpy as np
from mediapipe.tasks import python as mp_python
from mediapipe.tasks.python import vision

_MODEL_DIR = Path.home() / ".memecloud" / "models"

# 全局单例（线程安全由调用方保证）
_pose_landmarker: vision.PoseLandmarker | None = None
_hand_landmarker: vision.HandLandmarker | None = None
_initialized = False


def init_extractor():
    """加载模型（耗时 ~2-5s），在 FastAPI lifespan 中调用"""
    global _pose_landmarker, _hand_landmarker, _initialized
    if _initialized:
        return

    pose_path = _MODEL_DIR / "pose_landmarker_lite.task"
    hand_path = _MODEL_DIR / "hand_landmarker.task"

    if not pose_path.exists():
        raise FileNotFoundError(f"Pose model not found: {pose_path}")
    if not hand_path.exists():
        raise FileNotFoundError(f"Hand model not found: {hand_path}")

    pose_opts = vision.PoseLandmarkerOptions(
        base_options=mp_python.BaseOptions(model_asset_path=str(pose_path)),
        running_mode=vision.RunningMode.IMAGE,
        num_poses=1,
    )
    hand_opts = vision.HandLandmarkerOptions(
        base_options=mp_python.BaseOptions(model_asset_path=str(hand_path)),
        running_mode=vision.RunningMode.IMAGE,
        num_hands=2,
    )

    _pose_landmarker = vision.PoseLandmarker.create_from_options(pose_opts)
    _hand_landmarker = vision.HandLandmarker.create_from_options(hand_opts)
    _initialized = True


def extract_gesture_features(image_bytes: bytes) -> list[float] | None:
    """从 JPEG/PNG 字节提取 22 维手势特征向量"""
    if not _initialized:
        init_extractor()

    nparr = np.frombuffer(image_bytes, np.uint8)
    img_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if img_bgr is None:
        return None
    img_rgb = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2RGB)

    # --- Pose ---
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=img_rgb)
    pose_result = _pose_landmarker.detect(mp_image)
    if not pose_result.pose_landmarks:
        return None
    pl = pose_result.pose_landmarks[0]  # 33 NormalizedLandmark

    # 身体中心 + 肩宽
    cx = (pl[11].x + pl[12].x) / 2
    cy = (pl[11].y + pl[12].y) / 2
    sw = hypot(pl[11].x - pl[12].x, pl[11].y - pl[12].y) + 1e-6

    # 5 个关键点: nose(0), L-elbow(13), R-elbow(14), L-wrist(15), R-wrist(16)
    f = []
    for i in [0, 13, 14, 15, 16]:
        f.append((pl[i].x - cx) / sw)
        f.append((pl[i].y - cy) / sw)

    # --- Hands ---
    izq = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
    der = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0]

    hand_result = _hand_landmarker.detect(mp_image)
    if hand_result.hand_landmarks and hand_result.handedness:
        for idx, lm_list in enumerate(hand_result.hand_landmarks):
            lm = lm_list  # 21 NormalizedLandmark
            is_left = hand_result.handedness[idx][0].category_name == "Left"

            # dedosEstado — matches JS dedosEstado()
            tip_ids = [8, 12, 16, 20]
            mid_ids = [6, 10, 14, 18]
            if is_left:
                open_val = 1.0 if lm[4].x > lm[3].x else 0.0
            else:
                open_val = 1.0 if lm[4].x < lm[3].x else 0.0
            ded = [open_val]
            for j in range(4):
                ded.append(1.0 if lm[tip_ids[j]].y < lm[mid_ids[j]].y else 0.0)

            # pinch
            palma = hypot(lm[0].x - lm[9].x, lm[0].y - lm[9].y) + 1e-6
            pinch = hypot(lm[4].x - lm[8].x, lm[4].y - lm[8].y) / palma

            state = ded + [pinch]
            if is_left:
                izq = state
            else:
                der = state

    f.extend(izq)
    f.extend(der)
    return f  # 22 维
