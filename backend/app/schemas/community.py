"""社区广场 — Pydantic 请求/响应模型"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


# ── 请求 ──

class PostCreateRequest(BaseModel):
    image_url: str = Field(..., max_length=512)
    thumbnail_url: Optional[str] = Field(None, max_length=512)
    caption: Optional[str] = Field(None, max_length=200)
    tags: list[str] = Field(default_factory=list, max_length=10)


class CommentCreateRequest(BaseModel):
    content: str = Field(..., min_length=1, max_length=500)


# ── 响应 ──

class AuthorBrief(BaseModel):
    id: int
    username: str
    nickname: Optional[str] = None
    avatar_url: Optional[str] = None


class CommentBrief(BaseModel):
    id: int
    content: str
    author: AuthorBrief
    created_at: str


class PostBrief(BaseModel):
    id: int
    image_url: str
    thumbnail_url: Optional[str] = None
    caption: Optional[str] = None
    tags: list[str]
    author: AuthorBrief
    like_count: int
    comment_count: int
    is_liked: bool = False
    created_at: str


class PostDetail(BaseModel):
    id: int
    image_url: str
    thumbnail_url: Optional[str] = None
    caption: Optional[str] = None
    tags: list[str]
    author: AuthorBrief
    like_count: int
    comment_count: int
    is_liked: bool = False
    created_at: str
    comments: list[CommentBrief] = Field(default_factory=list)


class PaginatedPosts(BaseModel):
    items: list[PostBrief]
    page: int
    size: int
    has_more: bool
