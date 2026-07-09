"""Pydantic 请求/响应模型"""

from typing import Optional

from pydantic import BaseModel, Field


# ── 请求 ──

class RegisterRequest(BaseModel):
    username: str = Field(..., min_length=3, max_length=32, examples=["testuser"])
    nickname: str = Field(..., min_length=1, max_length=32, examples=["测试用户"])
    password: str = Field(..., min_length=6, max_length=64, examples=["123456"])


class LoginRequest(BaseModel):
    username: str = Field(..., min_length=1, examples=["testuser"])
    password: str = Field(..., min_length=1, examples=["123456"])


# ── 响应 ──

class UserBrief(BaseModel):
    """用户简要信息（不暴露密码）"""
    id: int
    username: str
    nickname: Optional[str] = None
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    points: int
    follower_count: int
    following_count: int


class AuthResponse(BaseModel):
    """登录/注册成功后返回"""
    token: str
    user: UserBrief


class ApiResponse(BaseModel):
    """通用 API 响应"""
    code: int = 0
    msg: str = "ok"
    data: Optional[dict] = None
