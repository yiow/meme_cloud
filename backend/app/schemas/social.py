"""社交互动 — Pydantic 请求/响应模型"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


# ── 用户详情 ──

class UserProfile(BaseModel):
    id: int
    username: str
    nickname: Optional[str] = None
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    points: int = 0
    follower_count: int = 0
    following_count: int = 0
    post_count: int = 0
    created_at: str = ""


class FollowUserBrief(BaseModel):
    id: int
    username: str
    nickname: Optional[str] = None
    avatar_url: Optional[str] = None
    bio: Optional[str] = None
    is_followed: bool = False


# ── 排行榜 ──

class RankPost(BaseModel):
    rank: int
    id: int
    image_url: str
    thumbnail_url: Optional[str] = None
    caption: Optional[str] = None
    author_id: int
    author_name: str
    author_avatar: Optional[str] = None
    like_count: int
    comment_count: int
    is_liked: bool = False


# ── 关注 ──

class FollowToggleResponse(BaseModel):
    is_followed: bool
    follower_count: int
    following_count: int


# ── 话题挑战 ──

class TopicBrief(BaseModel):
    id: int
    title: str
    description: Optional[str] = None
    cover_url: Optional[str] = None
    start_time: str
    end_time: str
    status: int
    submission_count: int = 0


class TopicCreateRequest(BaseModel):
    title: str = Field(..., min_length=1, max_length=100)
    description: Optional[str] = Field(None, max_length=500)
    cover_url: Optional[str] = Field(None, max_length=512)
    end_time: str  # ISO datetime string


class TopicSubmitRequest(BaseModel):
    post_id: int


class TopicSubmissionBrief(BaseModel):
    id: int
    topic_id: int
    user_id: int
    post_id: int
    post_image_url: str
    post_caption: Optional[str] = None
    user_name: str
    user_avatar: Optional[str] = None
    vote_count: int
    created_at: str
