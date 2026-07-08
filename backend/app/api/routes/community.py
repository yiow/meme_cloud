"""社区广场路由 — 发布/瀑布流/详情/点赞/评论"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id, require_user
from app.core.database import get_db
from app.schemas.community import CommentCreateRequest, PostCreateRequest
from app.schemas.user import ApiResponse
from app.services import community as svc

router = APIRouter(prefix="/api/community", tags=["社区广场"])


@router.post("/posts", response_model=ApiResponse)
def create_post(
    req: PostCreateRequest,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """发布社区帖子"""
    post = svc.create_post(db, user_id, req)
    return ApiResponse(msg="发布成功", data={"id": post.id})


@router.delete("/posts/{post_id}", response_model=ApiResponse)
def delete_post(
    post_id: int,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """删除自己的帖子"""
    ok = svc.delete_post(db, post_id, user_id)
    if not ok:
        raise HTTPException(status_code=404, detail="帖子不存在或无权操作")
    return ApiResponse(msg="已删除")


@router.get("/posts", response_model=ApiResponse)
def list_posts(
    sort: str = "recommend",
    page: int = 1,
    size: int = 20,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """瀑布流列表"""
    result = svc.get_feed(db, sort=sort, page=page, size=size, current_user_id=user_id)
    return ApiResponse(data=result.model_dump())


@router.get("/posts/{post_id}", response_model=ApiResponse)
def get_post(
    post_id: int,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """帖子详情（含评论）"""
    detail = svc.get_post_detail(db, post_id, current_user_id=user_id)
    if not detail:
        raise HTTPException(status_code=404, detail="帖子不存在")
    return ApiResponse(data=detail.model_dump())


@router.post("/posts/{post_id}/like", response_model=ApiResponse)
def toggle_like(
    post_id: int,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """点赞/取消点赞"""
    liked = svc.toggle_like(db, post_id, user_id)
    return ApiResponse(data={"is_liked": liked})


@router.post("/posts/{post_id}/comments", response_model=ApiResponse)
def add_comment(
    post_id: int,
    req: CommentCreateRequest,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """发表评论"""
    comment = svc.create_comment(db, post_id, user_id, req.content)
    return ApiResponse(msg="评论成功", data={"id": comment.id})
