"""用户业务逻辑"""

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.security import create_access_token, hash_password, verify_password
from app.models.user import User
from app.schemas.user import AuthResponse, UserBrief


class AuthError(Exception):
    """认证异常"""
    pass


class DuplicateUserError(Exception):
    """用户名重复"""
    pass


def register_user(db: Session, username: str, nickname: str, password: str) -> AuthResponse:
    """注册新用户"""
    # 查重
    stmt = select(User).where(User.username == username)
    existing = db.execute(stmt).scalar_one_or_none()
    if existing:
        raise DuplicateUserError(f"用户名 '{username}' 已被注册")

    user = User(
        username=username,
        nickname=nickname,
        password_hash=hash_password(password),
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    token = create_access_token(user.id, user.username)
    return AuthResponse(token=token, user=_to_brief(user))


def login_user(db: Session, username: str, password: str) -> AuthResponse:
    """用户登录"""
    stmt = select(User).where(User.username == username)
    user = db.execute(stmt).scalar_one_or_none()

    if not user or not verify_password(password, user.password_hash):
        raise AuthError("用户名或密码错误")

    token = create_access_token(user.id, user.username)
    return AuthResponse(token=token, user=_to_brief(user))


def get_user_by_id(db: Session, user_id: int) -> User | None:
    return db.get(User, user_id)


def _to_brief(user: User) -> UserBrief:
    return UserBrief(
        id=user.id,
        username=user.username,
        nickname=user.nickname,
        avatar_url=user.avatar_url,
        bio=user.bio,
        points=user.points,
        follower_count=user.follower_count,
        following_count=user.following_count,
    )
