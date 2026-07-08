"""DanmakuRoom ORM model — maps danmaku_rooms table"""

from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base


class DanmakuRoom(Base):
    __tablename__ = "danmaku_rooms"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    creator_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    max_users: Mapped[int] = mapped_column(Integer, default=50)
    status: Mapped[int] = mapped_column(Integer, default=1, comment="0=closed, 1=active")
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    last_active_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now()
    )
