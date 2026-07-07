"""表情云库 — FastAPI 入口"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.routes import auth

app = FastAPI(
    title="表情云库 API",
    description="MemeCloud — 多模态表情包智能检索与社交分享平台",
    version="0.1.0",
)

# CORS — 允许 Android 模拟器 / 前端跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(auth.router)


@app.get("/")
def root():
    return {"service": "MemeCloud API", "version": "0.1.0"}


@app.get("/health")
def health():
    return {"status": "ok"}
