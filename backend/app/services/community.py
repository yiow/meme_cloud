"""社区广场业务逻辑"""

from typing import Any, Optional

from sqlalchemy import and_, func, select
from sqlalchemy.orm import Session

from app.models.community import Comment, CommunityPost, Like
from app.models.social import Follow
from app.schemas.community import (
    AuthorBrief,
    CommentBrief,
    PaginatedPosts,
    PostBrief,
    PostCreateRequest,
    PostDetail,
)


def _author_brief(obj: Any) -> AuthorBrief:
    user = obj.user
    return AuthorBrief(
        id=user.id,
        username=user.username,
        nickname=user.nickname,
        avatar_url=user.avatar_url,
    )


def _parse_tags(tags_str: Optional[str]) -> list[str]:
    if not tags_str:
        return []
    return [t.strip() for t in tags_str.split(",") if t.strip()]


def create_post(
    db: Session, user_id: int, req: PostCreateRequest
) -> CommunityPost:
    post = CommunityPost(
        user_id=user_id,
        image_url=req.image_url,
        thumbnail_url=req.thumbnail_url,
        caption=req.caption,
        tags=",".join(req.tags) if req.tags else None,
    )
    db.add(post)
    db.commit()
    db.refresh(post)
    return post


def delete_post(db: Session, post_id: int, user_id: int) -> bool:
    post = db.get(CommunityPost, post_id)
    if not post or post.user_id != user_id or post.is_deleted:
        return False
    post.is_deleted = True
    db.commit()
    return True


def get_feed(
    db: Session,
    sort: str,
    page: int,
    size: int,
    current_user_id: int = 0,
) -> PaginatedPosts:
    base = select(CommunityPost).where(CommunityPost.is_deleted == False)

    if sort == "follow" and current_user_id:
        following_ids = db.execute(
            select(Follow.following_id).where(Follow.follower_id == current_user_id)
        ).scalars().all()
        if following_ids:
            base = base.where(CommunityPost.user_id.in_(following_ids))
        else:
            base = base.where(CommunityPost.user_id == 0)  # 没关注任何人时返回空
    elif sort == "recommend":
        base = base.order_by(CommunityPost.like_count.desc(), CommunityPost.created_at.desc())
    else:
        base = base.order_by(CommunityPost.created_at.desc())

    total_query = select(func.count()).select_from(base.subquery())
    total = db.execute(total_query).scalar() or 0

    rows = db.execute(base.offset((page - 1) * size).limit(size)).scalars().all()

    items = []
    for post in rows:
        liked = False
        if current_user_id:
            liked = db.execute(
                select(Like).where(
                    and_(Like.post_id == post.id, Like.user_id == current_user_id)
                )
            ).scalar_one_or_none() is not None
        items.append(
            PostBrief(
                id=post.id,
                image_url=post.image_url,
                thumbnail_url=post.thumbnail_url,
                caption=post.caption,
                tags=_parse_tags(post.tags),
                author=_author_brief(post),
                like_count=post.like_count,
                comment_count=post.comment_count,
                is_liked=liked,
                created_at=post.created_at.isoformat() if post.created_at else "",
            )
        )

    return PaginatedPosts(
        items=items,
        page=page,
        size=size,
        has_more=page * size < total,
    )


def get_post_detail(db: Session, post_id: int, current_user_id: int = 0) -> Optional[PostDetail]:
    post = db.get(CommunityPost, post_id)
    if not post or post.is_deleted:
        return None

    liked = False
    if current_user_id:
        liked = db.execute(
            select(Like).where(
                and_(Like.post_id == post_id, Like.user_id == current_user_id)
            )
        ).scalar_one_or_none() is not None

    comment_rows = db.execute(
        select(Comment)
        .where(Comment.post_id == post_id)
        .order_by(Comment.created_at.asc())
    ).scalars().all()

    comments = [
        CommentBrief(
            id=c.id,
            content=c.content,
            author=_author_brief(c),
            created_at=c.created_at.isoformat() if c.created_at else "",
        )
        for c in comment_rows
    ]

    return PostDetail(
        id=post.id,
        image_url=post.image_url,
        thumbnail_url=post.thumbnail_url,
        caption=post.caption,
        tags=_parse_tags(post.tags),
        author=_author_brief(post),
        like_count=post.like_count,
        comment_count=post.comment_count,
        is_liked=liked,
        created_at=post.created_at.isoformat() if post.created_at else "",
        comments=comments,
    )


def toggle_like(db: Session, post_id: int, user_id: int) -> bool:
    existing = db.execute(
        select(Like).where(
            and_(Like.post_id == post_id, Like.user_id == user_id)
        )
    ).scalar_one_or_none()

    post = db.get(CommunityPost, post_id)
    if not post:
        return False

    if existing:
        db.delete(existing)
        post.like_count = max(0, post.like_count - 1)
        db.commit()
        return False
    else:
        db.add(Like(post_id=post_id, user_id=user_id))
        post.like_count += 1
        db.commit()
        return True


def create_comment(db: Session, post_id: int, user_id: int, content: str) -> Comment:
    comment = Comment(post_id=post_id, user_id=user_id, content=content)
    db.add(comment)

    post = db.get(CommunityPost, post_id)
    if post:
        post.comment_count += 1

    db.commit()
    db.refresh(comment)
    return comment
