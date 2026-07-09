"""社交互动路由 — 排行榜 / 关注 / 话题挑战 / 用户主页"""

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.api.deps import get_current_user_id, require_user
from app.core.database import get_db
from app.schemas.social import TopicCreateRequest, TopicSubmitRequest
from app.schemas.user import ApiResponse
from app.services import social as svc

router = APIRouter(prefix="/api", tags=["社交互动"])


# ═══════════════════════════════════════════════
#  排行榜
# ═══════════════════════════════════════════════

@router.get("/ranking", response_model=ApiResponse)
def get_ranking(
    period: str = "today",
    size: int = 50,
    user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """排行榜：today / week / all"""
    if period not in ("today", "week", "all"):
        raise HTTPException(status_code=400, detail="period 参数无效，可选: today, week, all")
    data = svc.get_ranking(db, period=period, current_user_id=user_id, size=size)
    return ApiResponse(data={"items": [r.model_dump() for r in data]})


# ═══════════════════════════════════════════════
#  关注系统
# ═══════════════════════════════════════════════

@router.post("/follow/{following_id}", response_model=ApiResponse)
def toggle_follow(
    following_id: int,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """关注/取消关注"""
    try:
        result = svc.toggle_follow(db, user_id, following_id)
        return ApiResponse(data=result.model_dump())
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/follow/following/{user_id}", response_model=ApiResponse)
def get_following(
    user_id: int,
    current_user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """某人关注的用户列表"""
    data = svc.get_following_list(db, user_id, current_user_id=current_user_id)
    return ApiResponse(data={"items": [u.model_dump() for u in data]})


@router.get("/follow/followers/{user_id}", response_model=ApiResponse)
def get_followers(
    user_id: int,
    current_user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """某人的粉丝列表"""
    data = svc.get_follower_list(db, user_id, current_user_id=current_user_id)
    return ApiResponse(data={"items": [u.model_dump() for u in data]})


# ═══════════════════════════════════════════════
#  用户主页
# ═══════════════════════════════════════════════

@router.get("/user/profile/{user_id}", response_model=ApiResponse)
def get_user_profile(
    user_id: int,
    current_user_id: int = Depends(get_current_user_id),
    db: Session = Depends(get_db),
):
    """用户主页信息"""
    profile = svc.get_user_profile(db, user_id, current_user_id=current_user_id)
    if not profile:
        raise HTTPException(status_code=404, detail="用户不存在")
    return ApiResponse(data=profile.model_dump())


@router.get("/user/me", response_model=ApiResponse)
def get_my_profile(
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """当前登录用户主页"""
    profile = svc.get_user_profile(db, user_id, current_user_id=user_id)
    if not profile:
        raise HTTPException(status_code=404, detail="用户不存在")
    return ApiResponse(data=profile.model_dump())


# ═══════════════════════════════════════════════
#  话题挑战
# ═══════════════════════════════════════════════

@router.post("/topics", response_model=ApiResponse)
def create_topic(
    req: TopicCreateRequest,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """创建话题（任何登录用户可创建）"""
    topic = svc.create_topic(db, req.title, req.description, req.cover_url, req.end_time)
    return ApiResponse(msg="话题创建成功", data={"id": topic.id})


@router.get("/topics", response_model=ApiResponse)
def list_topics(
    include_ended: bool = False,
    db: Session = Depends(get_db),
):
    """话题列表"""
    topics = svc.list_topics(db, include_ended=include_ended)
    return ApiResponse(data={"items": [t.model_dump() for t in topics]})


@router.get("/topics/{topic_id}", response_model=ApiResponse)
def get_topic(
    topic_id: int,
    db: Session = Depends(get_db),
):
    """话题详情"""
    detail = svc.get_topic_detail(db, topic_id)
    if not detail:
        raise HTTPException(status_code=404, detail="话题不存在")
    return ApiResponse(data=detail.model_dump())


@router.post("/topics/{topic_id}/submit", response_model=ApiResponse)
def submit_to_topic(
    topic_id: int,
    req: TopicSubmitRequest,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """参与话题"""
    try:
        submission = svc.submit_to_topic(db, topic_id, user_id, req.post_id)
        return ApiResponse(msg="投稿成功", data={"id": submission.id})
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/topics/{topic_id}/submissions", response_model=ApiResponse)
def get_submissions(
    topic_id: int,
    page: int = 1,
    size: int = 20,
    db: Session = Depends(get_db),
):
    """话题投稿列表（按票数降序）"""
    items = svc.get_topic_submissions(db, topic_id, page=page, size=size)
    return ApiResponse(data={"items": [s.model_dump() for s in items]})


@router.post("/topics/submissions/{submission_id}/vote", response_model=ApiResponse)
def vote_submission(
    submission_id: int,
    user_id: int = Depends(require_user),
    db: Session = Depends(get_db),
):
    """给投稿投票"""
    try:
        count = svc.vote_submission(db, submission_id, user_id)
        return ApiResponse(msg="投票成功", data={"vote_count": count})
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
