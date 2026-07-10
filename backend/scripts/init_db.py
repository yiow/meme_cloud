"""
MemeCloud - 数据库一键初始化脚本
用法: python scripts/init_db.py
      python scripts/init_db.py --host localhost --port 3306 --user root --password root

作用:
  1. 创建 memecloud 数据库（DROP + CREATE）
  2. 从 ORM 模型自动建表（19 张表）
  3. 导入测试种子数据
"""

import argparse, sys, os, hashlib
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

parser = argparse.ArgumentParser()
parser.add_argument("--host", default="localhost")
parser.add_argument("--port", type=int, default=3306)
parser.add_argument("--user", default="root")
parser.add_argument("--password", default="root")
parser.add_argument("--database", default="memecloud")
args = parser.parse_args()

from sqlalchemy import create_engine, text

# Step 1: Create database
print("=== 1/3  Creating database ===")
root_url = f"mysql+pymysql://{args.user}:{args.password}@{args.host}:{args.port}"
root_engine = create_engine(root_url)
with root_engine.connect() as conn:
    conn.execute(text("SET FOREIGN_KEY_CHECKS = 0"))
    conn.execute(text(f"DROP DATABASE IF EXISTS `{args.database}`"))
    conn.execute(text(f"CREATE DATABASE `{args.database}` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci"))
    conn.execute(text("SET FOREIGN_KEY_CHECKS = 1"))
    conn.commit()
print(f"  Database `{args.database}` created")

# Step 2: Create tables from ORM models
print("\n=== 2/3  Creating tables ===")
from app.core.database import Base
db_url = f"mysql+pymysql://{args.user}:{args.password}@{args.host}:{args.port}/{args.database}?charset=utf8mb4"
engine = create_engine(db_url)

from app.models.user import User
from app.models.community import CommunityPost, Comment, Like
from app.models.social import Follow, Topic, TopicSubmission, TopicVote
from app.models.danmaku_room import DanmakuRoom
from app.models.emoji import Emoji
from app.models.favorite import Favorite
from app.models.tag import Tag, EmojiTag
from app.models.search_history import SearchHistory
from app.models.game import GameMatch, GameParticipant
from app.models.bounty import Bounty, BountySubmission
from app.models.notification import Notification
Base.metadata.create_all(bind=engine)
print(f"  Created {len(Base.metadata.sorted_tables)} tables")

# Step 3: Seed data
print("\n=== 3/3  Seeding data ===")
BCRYPT = "$2b$12$2gPfBUGcW4XStOHXcHbPseS0CQPxukyQQt2kGknrIEhV4DOfn4T4W"

with engine.connect() as conn:
    conn.execute(text("SET FOREIGN_KEY_CHECKS = 0"))

    conn.execute(
        text("INSERT IGNORE INTO users (username, password_hash, nickname, bio, points) VALUES "
             "('testuser', :p, '测试用户', '这个人很懒，什么都没写~', 100),"
             "('emojiking', :p, '表情帝', '热爱表情包创作', 200),"
             "('catlover', :p, '猫猫教主', '铲屎官一枚~', 150),"
             "('meme_maker', :p, '表情大师', '专业生产表情包', 300),"
             "('doutu_boss', :p, '斗图狂魔', '斗图从没输过', 500),"
             "('shenhui', :p, '神回复', '我就是那个神回复', 50),"
             "('newbie', :p, '萌新一号', '刚来，请多关照~', 0)"),
        {"p": BCRYPT}
    )
    conn.commit()
    user_ids = {r[1]: r[1] for r in conn.execute(text("SELECT id, id FROM users")).all()}
    uid = user_ids
    print("  - Users: 7")

    # Community posts (31 posts from Unsplash)
    posts_data = [
        (1, 'https://images.unsplash.com/photo-1560807707-8cc77767d783?w=400&h=500&fit=crop', 'Cute dog', '狗,可爱'),
        (2, 'https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=400&h=500&fit=crop', 'Sleepy dog', '狗,睡觉'),
        (2, 'https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=400&h=500&fit=crop', 'Smug cat', '猫,得意'),
        (3, 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400&h=500&fit=crop', 'Monday blues', '周一,崩溃'),
        (3, 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=400&h=500&fit=crop', 'Doggo approves', '开心'),
        (3, 'https://images.unsplash.com/photo-1507146426996-ef05306b995a?w=400&h=500&fit=crop', 'Puppy eyes', '可爱'),
        (3, 'https://images.unsplash.com/photo-1574158622682-e40e69881006?w=400&h=500&fit=crop', 'Surprised cat', '震惊'),
        (3, 'https://images.unsplash.com/photo-1518791841217-8f162f1e1131?w=400&h=500&fit=crop', 'Cool cat', '猫,酷'),
        (4, 'https://images.unsplash.com/photo-1561948955-570b270e7c36?w=400&h=500&fit=crop', 'Begging cat', '猫,乞求'),
        (4, 'https://images.unsplash.com/photo-1596854407944-bf87f85f138f?w=400&h=500&fit=crop', 'Happy cat', '猫,开心'),
        (4, 'https://images.unsplash.com/photo-1586671267731-da2cf3ceeb80?w=400&h=500&fit=crop', 'Curious', '好奇'),
        (4, 'https://images.unsplash.com/photo-1519052537078-e6302a4968d4?w=400&h=500&fit=crop', 'Smiling cat', '猫,微笑'),
        (4, 'https://images.unsplash.com/photo-1533743983669-94fa5c4338ec?w=400&h=500&fit=crop', 'Blushing', '害羞'),
        (5, 'https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=400&h=500&fit=crop', 'Funny face', '搞笑,表情'),
        (5, 'https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=400&h=500&fit=crop', 'Orange cat', '猫,橘猫'),
        (5, 'https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400&h=500&fit=crop', 'Sleepy cat', '猫,睡觉'),
        (5, 'https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400&h=500&fit=crop', 'Side eye cat', '猫,鄙视'),
        (5, 'https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=400&h=500&fit=crop', 'Grumpy cat', '猫,生气'),
        (5, 'https://images.unsplash.com/photo-1602491453631-e2a5ad90a131?w=400&h=500&fit=crop', 'Wide eyes', '震惊,瞪眼'),
        (5, 'https://images.unsplash.com/photo-1553882809-a4f57e595701?w=400&h=500&fit=crop', 'Excited', '兴奋'),
        (1, 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400&h=500&fit=crop', 'Derp face', '搞笑,呆'),
        (1, 'https://images.unsplash.com/photo-1589652717521-10c0d092dea9?w=400&h=500&fit=crop', 'Zoomies', '搞笑,猫'),
        (1, 'https://images.unsplash.com/photo-1608848461950-0fe51dfc41cb?w=400&h=500&fit=crop', 'Cat loaf', '猫,可爱'),
        (1, 'https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400&h=500&fit=crop', 'Oops', '尴尬'),
        (2, 'https://images.unsplash.com/photo-1513245543132-31f507417b26?w=400&h=500&fit=crop', 'Laughing', '开心'),
        (2, 'https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=400&h=500&fit=crop', 'Confused', '迷惑,问号'),
        (2, 'https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=400&h=500&fit=crop', 'Angry birb', '生气'),
        (2, 'https://images.unsplash.com/photo-1552944150-6dd1180e5999?w=400&h=500&fit=crop', 'Suspicious', '怀疑,眯眼'),
        (2, 'https://images.unsplash.com/photo-1517423568366-8b83523034fd?w=400&h=500&fit=crop', 'Judging you', '鄙视'),
        (3, 'https://images.unsplash.com/photo-1579165466741-7f35e4755660?w=400&h=500&fit=crop', 'Tongue out', '搞笑,吐舌'),
        (4, 'https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?w=400&h=500&fit=crop', 'Blep', '猫,吐舌'),
    ]
    print("  - Community posts: " + str(len(posts_data)))
    for uid_num, img, cap, tags in posts_data:
        u = uid(uid_num)
        if u:
            conn.execute(
                text("INSERT INTO community_posts (user_id, image_url, thumbnail_url, caption, tags, like_count, comment_count, is_deleted) "
                     "VALUES (:uid, :img, :img, :cap, :tags, FLOOR(RAND()*80+10), FLOOR(RAND()*8), 0)"),
                {"uid": u, "img": img, "cap": cap, "tags": tags}
            )

    # Topics (6)
    conn.execute(text(
        "INSERT INTO topics (title, description, status, start_time, end_time) VALUES "
        "('用表情包表达周一的心情','周一综合症发作现场',1, NOW()-INTERVAL 1 DAY, NOW()+INTERVAL 6 DAY),"
        "('我的精神状态 be like','用一张表情包形容',1, NOW()-INTERVAL 2 DAY, NOW()+INTERVAL 5 DAY),"
        "('打工人嘴替','那些打工人才敢说的话',2, NOW()-INTERVAL 10 DAY, NOW()-INTERVAL 3 DAY),"
        "('猫猫表情包大赏','晒出你最爱的猫猫表情包',2, NOW()-INTERVAL 14 DAY, NOW()-INTERVAL 7 DAY),"
        "('扎心瞬间','被现实暴击的那些瞬间',1, NOW()-INTERVAL 1 DAY, NOW()+INTERVAL 4 DAY),"
        "('真香预警','打脸来得太快的名场面',2, NOW()-INTERVAL 21 DAY, NOW()-INTERVAL 14 DAY)"
    ))
    print("  - Topics: 6")

    # Follow relationships (13)
    follows = [(1,2),(1,3),(1,5),(2,1),(2,3),(2,4),(3,1),(3,2),(4,1),(4,2),(4,3),(4,5),(5,1)]
    for f, g in follows:
        conn.execute(text("INSERT IGNORE INTO follows (follower_id, following_id) VALUES (:f, :g)"),
                     {"f": uid(f), "g": uid(g)})
    print("  - Follows: 13")

    # Topics, tags (10), etc.
    conn.execute(text("INSERT IGNORE INTO tags (name, usage_count) VALUES "
                      "('搞笑',1240),('打工人',890),('猫咪',756),('摸鱼',632),('周一',520),"
                      "('社恐',488),('吃瓜',401),('扎心',356),('熊猫人',312),('真香',201)"))
    print("  - Tags: 10")
    
    # Emojis (8)
    emoji_urls = [
        "https://images.unsplash.com/photo-1560807707-8cc77767d783?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1574158622682-e40e69881006?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1548247416-ec66f4900b2e?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1513245543132-31f507417b26?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1506755594592-366d8f9ab00d?w=300&h=300&fit=crop",
        "https://images.unsplash.com/photo-1577023311546-cdc07a8454ae?w=300&h=300&fit=crop",
    ]
    emoji_descs = ["周一早上", "打工人", "猫咪歪头", "真的假的", "给你点赞", "哈哈哈", "疑惑", "愤怒小鸟"]
    thumb_urls = [u.replace("w=300&h=300", "w=150&h=150") for u in emoji_urls]
    fake_md5 = hashlib.md5("seed".encode()).hexdigest()
    for i, (url, thumb, desc) in enumerate(zip(emoji_urls, thumb_urls, emoji_descs), start=1):
        conn.execute(
            text("INSERT IGNORE INTO emojis (id, file_url, thumbnail_url, file_md5, uploader_id, description, source_type, status, like_count, collect_count) "
                 "VALUES (:id, :url, :thumb, :md5, :uid, :desc, 1, 1, FLOOR(RAND()*50+5), FLOOR(RAND()*10))"),
            {"id": i, "url": url, "thumb": thumb, "md5": fake_md5 + str(i), "uid": uid(i % 5 + 1), "desc": desc}
        )
    print("  - Emojis: 8")

    # Recalculate follow counts
    conn.execute(text("UPDATE users u SET following_count=(SELECT COUNT(*) FROM follows WHERE follower_id=u.id),"
                      "follower_count=(SELECT COUNT(*) FROM follows WHERE following_id=u.id)"))
    conn.execute(text("SET FOREIGN_KEY_CHECKS = 1"))
    conn.commit()

print("\nDone!")
print("Start server: cd backend && python -m uvicorn app.main:app --host 0.0.0.0 --port 9000")
print("Test account: testuser / 123456")
