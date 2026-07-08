"""认证依赖 — 从 Authorization Header 中提取当前用户"""

from typing import Optional

from fastapi import Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.core.security import decode_access_token
from app.models.user import User

security_scheme = HTTPBearer(auto_error=False)


def get_current_user_id(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_scheme),
) -> int:
    """从 JWT 中解析 user_id，验证失败返回 0（游客）"""
    if credentials is None:
        return 0
    payload = decode_access_token(credentials.credentials)
    if payload is None:
        return 0
    try:
        return int(payload.get("sub", 0))
    except (ValueError, TypeError):
        return 0


def require_user(user_id: int = Depends(get_current_user_id)) -> int:
    """强制要求登录，否则 401"""
    if user_id == 0:
        raise HTTPException(status_code=401, detail="请先登录")
    return user_id
