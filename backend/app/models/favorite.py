"""Favorite ORM model — maps favorites table"""

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class Favorite(Base):
    __tablename__ = "favorites"

    user_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("users.id"), primary_key=True)
    emoji_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("emojis.id"), primary_key=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
