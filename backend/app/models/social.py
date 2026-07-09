"""社交互动 ORM 模型 — follows / topics / topic_submissions"""

from datetime import datetime
from typing import Optional

from sqlalchemy import BigInteger, DateTime, ForeignKey, Integer, String, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.models.user import User


class Follow(Base):
    __tablename__ = "follows"

    follower_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("users.id"), primary_key=True)
    following_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("users.id"), primary_key=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    follower: Mapped[User] = relationship("User", foreign_keys=[follower_id], lazy="joined")
    following: Mapped[User] = relationship("User", foreign_keys=[following_id], lazy="joined")


class Topic(Base):
    __tablename__ = "topics"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[Optional[str]] = mapped_column(String(500), default=None)
    cover_url: Mapped[Optional[str]] = mapped_column(String(512), default=None)
    start_time: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())
    end_time: Mapped[datetime] = mapped_column(DateTime, nullable=False)
    status: Mapped[int] = mapped_column(Integer, default=1)  # 0=结束, 1=进行中


class TopicSubmission(Base):
    __tablename__ = "topic_submissions"

    id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    topic_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("topics.id"), nullable=False)
    user_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("users.id"), nullable=False)
    post_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("community_posts.id"), nullable=False)
    vote_count: Mapped[int] = mapped_column(Integer, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    user: Mapped[User] = relationship("User", lazy="joined")
    post: Mapped["CommunityPost"] = relationship("CommunityPost", lazy="joined")


class TopicVote(Base):
    __tablename__ = "topic_votes"

    submission_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("topic_submissions.id"), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger, ForeignKey("users.id"), primary_key=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, server_default=func.now())

    __table_args__ = (UniqueConstraint("submission_id", "user_id", name="uq_topic_vote_user"),)
