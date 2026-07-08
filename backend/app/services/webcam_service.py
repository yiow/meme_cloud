"""
PC 端摄像头服务 — 绕过 AVD 虚拟摄像头透传的偏移/裁切问题
直接在 Windows 上用 OpenCV 抓取 webcam，通过 HTTP 提供给 Android 前端
"""
import threading
import time
from pathlib import Path

import cv2
import numpy as np

_lock = threading.Lock()
_cap: cv2.VideoCapture | None = None
_latest_frame: bytes | None = None  # JPEG 字节
_last_fps = 0.0
_running = False
_thread: threading.Thread | None = None


def start_webcam(camera_index: int = 0, fps: int = 15, resolution: tuple = (640, 480)):
    """后台线程持续抓取 webcam 帧"""
    global _cap, _latest_frame, _running, _thread, _last_fps

    with _lock:
        if _running:
            return  # 已经在跑

        _cap = cv2.VideoCapture(camera_index, cv2.CAP_DSHOW)
        if not _cap.isOpened():
            _cap = cv2.VideoCapture(camera_index)  # fallback without DSHOW
        if not _cap.isOpened():
            raise RuntimeError(f"无法打开摄像头 {camera_index}")

        _cap.set(cv2.CAP_PROP_FRAME_WIDTH, resolution[0])
        _cap.set(cv2.CAP_PROP_FRAME_HEIGHT, resolution[1])
        _cap.set(cv2.CAP_PROP_FPS, fps)

        actual_w = _cap.get(cv2.CAP_PROP_FRAME_WIDTH)
        actual_h = _cap.get(cv2.CAP_PROP_FRAME_HEIGHT)
        print(f"[WebcamService] 摄像头已启动: {int(actual_w)}x{int(actual_h)} @ {fps}fps")

        _running = True

    def _loop():
        global _cap, _running, _latest_frame, _last_fps
        frame_interval = 1.0 / max(fps, 1)
        while True:
            with _lock:
                if not _running:
                    break
                cap = _cap
            if cap is None:
                break

            ret, frame = cap.read()
            if not ret:
                time.sleep(0.1)
                continue

            # 编码为 JPEG
            _, jpg = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
            with _lock:
                _latest_frame = jpg.tobytes()
                _last_fps = 1.0 / max(time.time() - getattr(_loop, '_last_time', time.time() - frame_interval), 0.001)
                _loop._last_time = time.time()  # type: ignore

            time.sleep(frame_interval)

        # 清理
        with _lock:
            if _cap:
                _cap.release()
                _cap = None

    _loop._last_time = time.time()  # type: ignore
    _thread = threading.Thread(target=_loop, daemon=True)
    _thread.start()


def stop_webcam():
    """停止摄像头采集"""
    global _running, _cap
    with _lock:
        _running = False
    if _thread:
        _thread.join(timeout=2.0)


def get_latest_frame() -> bytes | None:
    """获取最新一帧 (JPEG bytes)，供 HTTP 接口使用"""
    with _lock:
        return _latest_frame


def webcam_status() -> dict:
    with _lock:
        return {
            "running": _running,
            "fps": round(_last_fps, 1),
            "has_frame": _latest_frame is not None,
        }
