"""表情云库 — FastAPI 入口"""
 #uvicorn app.main:app --host 0.0.0.0 --port 9000
 #Get-NetTCPConnection -LocalPort 9000 | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from app.api.routes import auth, match
from app.services import matcher, mediapipe_extractor, webcam_service

# 表情包图片目录
MEME_IMG_DIR = Path(__file__).resolve().parent.parent.parent / "meme_match" / "memes"


@asynccontextmanager
async def lifespan(app: FastAPI):
    """启动时初始化：匹配引擎 + MediaPipe 模型 + PC 摄像头"""
    matcher.init_matcher()
    mediapipe_extractor.init_extractor()
    webcam_service.start_webcam(camera_index=0, fps=15, resolution=(640, 480))
    yield
    webcam_service.stop_webcam()

from app.api.routes import auth, community, social

app = FastAPI(
    title="表情云库 API",
    description="MemeCloud — 多模态表情包智能检索与社交分享平台",
    version="0.2.0",
    lifespan=lifespan,
)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 静态文件 — 表情包图片
if MEME_IMG_DIR.exists():
    app.mount("/static/memes", StaticFiles(directory=str(MEME_IMG_DIR)), name="memes")

# 路由
app.include_router(auth.router)
app.include_router(community.router)
app.include_router(social.router)


@app.get("/")
def root():
    return {"service": "MemeCloud API", "version": "0.2.0"}


@app.get("/health")
def health():
    return {"status": "ok"}
