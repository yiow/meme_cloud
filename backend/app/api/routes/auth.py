"""认证路由 — POST /api/auth/register  POST /api/auth/login"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.database import get_db
from app.schemas.user import ApiResponse, LoginRequest, RegisterRequest
from app.services.user import AuthError, DuplicateUserError, login_user, register_user

router = APIRouter(prefix="/api/auth", tags=["认证"])


@router.post("/register", response_model=ApiResponse)
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    """用户注册（仅需用户名 + 昵称 + 密码）"""
    try:
        result = register_user(db, req.username, req.nickname, req.password)
        return ApiResponse(msg="注册成功", data=result.model_dump())
    except DuplicateUserError as e:
        return ApiResponse(code=409, msg=str(e))


@router.post("/login", response_model=ApiResponse)
def login(req: LoginRequest, db: Session = Depends(get_db)):
    """用户名 + 密码登录"""
    try:
        result = login_user(db, req.username, req.password)
        return ApiResponse(msg="登录成功", data=result.model_dump())
    except AuthError as e:
        return ApiResponse(code=401, msg=str(e))
