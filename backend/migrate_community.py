"""建表脚本 — 创建 community_posts / comments / likes 表"""

from app.core.database import engine
from app.models import CommunityPost, Comment, Like

CommunityPost.metadata.create_all(bind=engine, tables=[
    CommunityPost.__table__,
    Comment.__table__,
    Like.__table__,
])
print("社区广场表创建完成")
