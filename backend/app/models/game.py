"""模仿大赛 ORM 模型 — game_matches / game_participants"""

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class GameMatch(Base):
    __tablename__ = "game_matches"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    target_emoji_id: Mapped[str] = mapped_column(String(100), nullable=False)  # emoji key
    target_label: Mapped[str] = mapped_column(String(100), nullable=False)     # display name
    target_image: Mapped[str] = mapped_column(String(512), nullable=False)     # emoji image URL
    status: Mapped[int] = mapped_column(Integer, default=0)  # 0=匹配中 1=进行中 2=已结束
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())


class GameParticipant(Base):
    __tablename__ = "game_participants"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    match_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    score: Mapped[int] = mapped_column(Integer, default=0)
    feature_json: Mapped[str | None] = mapped_column(Text, default=None)  # 用户特征向量 JSON
    photo_url: Mapped[str | None] = mapped_column(String(512), default=None)
    photo_label: Mapped[str | None] = mapped_column(String(100), default=None)  # 识别到的标签
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    __table_args__ = (UniqueConstraint("match_id", "user_id"),)
