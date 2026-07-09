"""社交互动业务逻辑 — 排行榜 / 关注 / 话题挑战"""

from datetime import datetime, timedelta
from typing import Any, Optional

from sqlalchemy import and_, func, select, text
from sqlalchemy.orm import Session

from app.models.community import Comment, CommunityPost, Like
from app.models.social import Follow, Topic, TopicSubmission, TopicVote
from app.models.user import User
from app.schemas.community import AuthorBrief  # reused by _author_brief
from app.schemas.social import (
    FollowToggleResponse,
    FollowUserBrief,
    RankPost,
    TopicBrief,
    TopicSubmissionBrief,
    UserProfile,
)


# ── helpers ──

def _author_brief(user: User) -> AuthorBrief:
    return AuthorBrief(
        id=user.id,
        username=user.username,
        nickname=user.nickname,
        avatar_url=user.avatar_url,
    )


# ═══════════════════════════════════════════════
#  排行榜
# ═══════════════════════════════════════════════

def get_ranking(
    db: Session,
    period: str,
    current_user_id: int = 0,
    size: int = 50,
) -> list[RankPost]:
    """按时间段和点赞数排序返回帖子"""
    base = select(CommunityPost).where(CommunityPost.is_deleted == False)

    now = datetime.now()
    if period == "today":
        base = base.where(CommunityPost.created_at >= now.replace(hour=0, minute=0, second=0))
    elif period == "week":
        base = base.where(CommunityPost.created_at >= now - timedelta(days=7))

    base = base.order_by(CommunityPost.like_count.desc(), CommunityPost.created_at.desc()).limit(size)
    rows = db.execute(base).scalars().all()

    results = []
    for rank, post in enumerate(rows, start=1):
        liked = False
        if current_user_id:
            liked = db.execute(
                select(Like).where(and_(Like.post_id == post.id, Like.user_id == current_user_id))
            ).scalar_one_or_none() is not None

        author_name = post.user.nickname or post.user.username
        results.append(
            RankPost(
                rank=rank,
                id=post.id,
                image_url=post.image_url,
                thumbnail_url=post.thumbnail_url,
                caption=post.caption,
                author_id=post.user_id,
                author_name=author_name,
                author_avatar=post.user.avatar_url,
                like_count=post.like_count,
                comment_count=post.comment_count,
                is_liked=liked,
            )
        )
    return results


# ═══════════════════════════════════════════════
#  关注系统
# ═══════════════════════════════════════════════

def toggle_follow(db: Session, follower_id: int, following_id: int) -> FollowToggleResponse:
    """关注/取消关注，同步维护 follower_count 和 following_count"""
    if follower_id == following_id:
        raise ValueError("不能关注自己")

    existing = db.execute(
        select(Follow).where(
            and_(Follow.follower_id == follower_id, Follow.following_id == following_id)
        )
    ).scalar_one_or_none()

    follower_user = db.get(User, follower_id)
    following_user = db.get(User, following_id)

    if existing:
        db.delete(existing)
        if follower_user:
            follower_user.following_count = max(0, follower_user.following_count - 1)
        if following_user:
            following_user.follower_count = max(0, following_user.follower_count - 1)
        db.commit()
        return FollowToggleResponse(
            is_followed=False,
            follower_count=following_user.follower_count if following_user else 0,
            following_count=follower_user.following_count if follower_user else 0,
        )
    else:
        db.add(Follow(follower_id=follower_id, following_id=following_id))
        if follower_user:
            follower_user.following_count += 1
        if following_user:
            following_user.follower_count += 1
        db.commit()
        return FollowToggleResponse(
            is_followed=True,
            follower_count=following_user.follower_count if following_user else 0,
            following_count=follower_user.following_count if follower_user else 0,
        )


def get_following_list(db: Session, user_id: int, current_user_id: int = 0) -> list[FollowUserBrief]:
    """查询关注列表"""
    rows = db.execute(
        select(Follow).where(Follow.follower_id == user_id).order_by(Follow.created_at.desc())
    ).scalars().all()

    result = []
    for f in rows:
        u = f.following
        is_followed = False
        if current_user_id:
            is_followed = db.execute(
                select(Follow).where(
                    and_(Follow.follower_id == current_user_id, Follow.following_id == u.id)
                )
            ).scalar_one_or_none() is not None
        result.append(
            FollowUserBrief(
                id=u.id,
                username=u.username,
                nickname=u.nickname,
                avatar_url=u.avatar_url,
                bio=u.bio,
                is_followed=is_followed,
            )
        )
    return result


def get_follower_list(db: Session, user_id: int, current_user_id: int = 0) -> list[FollowUserBrief]:
    """查询粉丝列表"""
    rows = db.execute(
        select(Follow).where(Follow.following_id == user_id).order_by(Follow.created_at.desc())
    ).scalars().all()

    result = []
    for f in rows:
        u = f.follower
        is_followed = False
        if current_user_id:
            is_followed = db.execute(
                select(Follow).where(
                    and_(Follow.follower_id == current_user_id, Follow.following_id == u.id)
                )
            ).scalar_one_or_none() is not None
        result.append(
            FollowUserBrief(
                id=u.id,
                username=u.username,
                nickname=u.nickname,
                avatar_url=u.avatar_url,
                bio=u.bio,
                is_followed=is_followed,
            )
        )
    return result


# ═══════════════════════════════════════════════
#  用户主页
# ═══════════════════════════════════════════════

def get_user_profile(db: Session, user_id: int, current_user_id: int = 0) -> Optional[UserProfile]:
    user = db.get(User, user_id)
    if not user:
        return None

    post_count = db.execute(
        select(func.count()).select_from(CommunityPost).where(
            and_(CommunityPost.user_id == user_id, CommunityPost.is_deleted == False)
        )
    ).scalar() or 0

    return UserProfile(
        id=user.id,
        username=user.username,
        nickname=user.nickname,
        avatar_url=user.avatar_url,
        bio=user.bio,
        points=user.points,
        follower_count=user.follower_count,
        following_count=user.following_count,
        post_count=post_count,
        created_at=user.created_at.isoformat() if user.created_at else "",
    )


# ═══════════════════════════════════════════════
#  获取关注用户的帖子 IDs（社区关注流）
# ═══════════════════════════════════════════════

def get_following_user_ids(db: Session, user_id: int) -> list[int]:
    rows = db.execute(
        select(Follow.following_id).where(Follow.follower_id == user_id)
    ).scalars().all()
    return list(rows)


# ═══════════════════════════════════════════════
#  话题挑战
# ═══════════════════════════════════════════════

def create_topic(db: Session, title: str, description: Optional[str], cover_url: Optional[str], end_time_str: str) -> Topic:
    end_time = datetime.fromisoformat(end_time_str)
    topic = Topic(
        title=title,
        description=description,
        cover_url=cover_url,
        end_time=end_time,
        status=1,
    )
    db.add(topic)
    db.commit()
    db.refresh(topic)
    return topic


def list_topics(db: Session, include_ended: bool = False) -> list[TopicBrief]:
    base = select(Topic).order_by(Topic.status.desc(), Topic.start_time.desc())
    if not include_ended:
        base = base.where(Topic.status == 1)
    rows = db.execute(base).scalars().all()

    now = datetime.now()
    results = []
    for t in rows:
        sub_count = db.execute(
            select(func.count()).select_from(TopicSubmission).where(TopicSubmission.topic_id == t.id)
        ).scalar() or 0

        if t.status == 1 and t.end_time < now:
            t.status = 0
            db.commit()

        results.append(
            TopicBrief(
                id=t.id,
                title=t.title,
                description=t.description,
                cover_url=t.cover_url,
                start_time=t.start_time.isoformat() if t.start_time else "",
                end_time=t.end_time.isoformat() if t.end_time else "",
                status=t.status,
                submission_count=sub_count,
            )
        )
    # 按参与人数降序排列
    results.sort(key=lambda x: x.submission_count, reverse=True)
    return results


def get_topic_detail(db: Session, topic_id: int) -> Optional[TopicBrief]:
    t = db.get(Topic, topic_id)
    if not t:
        return None
    sub_count = db.execute(
        select(func.count()).select_from(TopicSubmission).where(TopicSubmission.topic_id == topic_id)
    ).scalar() or 0
    return TopicBrief(
        id=t.id,
        title=t.title,
        description=t.description,
        cover_url=t.cover_url,
        start_time=t.start_time.isoformat() if t.start_time else "",
        end_time=t.end_time.isoformat() if t.end_time else "",
        status=t.status,
        submission_count=sub_count,
    )


def submit_to_topic(db: Session, topic_id: int, user_id: int, post_id: int) -> TopicSubmission:
    """用户投稿到话题（每个用户每个话题可多次投稿）"""
    post = db.get(CommunityPost, post_id)
    if not post or post.is_deleted:
        raise ValueError("帖子不存在")
    if post.user_id != user_id:
        raise ValueError("只能投稿自己的帖子")

    submission = TopicSubmission(topic_id=topic_id, user_id=user_id, post_id=post_id)

    submission = TopicSubmission(topic_id=topic_id, user_id=user_id, post_id=post_id)
    db.add(submission)
    db.commit()
    db.refresh(submission)
    return submission


def get_topic_submissions(db: Session, topic_id: int, page: int = 1, size: int = 20) -> list[TopicSubmissionBrief]:
    rows = db.execute(
        select(TopicSubmission)
        .where(TopicSubmission.topic_id == topic_id)
        .order_by(TopicSubmission.vote_count.desc(), TopicSubmission.created_at.desc())
        .offset((page - 1) * size)
        .limit(size)
    ).scalars().all()

    results = []
    for s in rows:
        results.append(
            TopicSubmissionBrief(
                id=s.id,
                topic_id=s.topic_id,
                user_id=s.user_id,
                post_id=s.post_id,
                post_image_url=s.post.image_url,
                post_caption=s.post.caption,
                user_name=s.user.nickname or s.user.username,
                user_avatar=s.user.avatar_url,
                vote_count=s.vote_count,
                created_at=s.created_at.isoformat() if s.created_at else "",
            )
        )
    return results


def vote_submission(db: Session, submission_id: int, user_id: int) -> int:
    """投票，每人只能投一次"""
    submission = db.get(TopicSubmission, submission_id)
    if not submission:
        raise ValueError("投稿不存在")

    existing = db.execute(
        select(TopicVote).where(
            and_(TopicVote.submission_id == submission_id, TopicVote.user_id == user_id)
        )
    ).scalar_one_or_none()
    if existing:
        raise ValueError("你已经投过票了")

    db.add(TopicVote(submission_id=submission_id, user_id=user_id))
    submission.vote_count += 1
    db.commit()
    return submission.vote_count
