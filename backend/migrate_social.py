"""模块四迁移 — 创建 social 相关表 + 种子话题"""

from datetime import datetime, timedelta

from app.core.database import engine, SessionLocal
from app.models.social import Follow, Topic, TopicSubmission


def migrate():
    # 删除旧的 topic_submissions（如果存在），重建
    with engine.connect() as conn:
        from sqlalchemy import text
        # 删除旧表（FK 顺序：先删子表）
        conn.execute(text("DROP TABLE IF EXISTS topic_submissions"))
        conn.execute(text("DROP TABLE IF EXISTS topics"))
        conn.execute(text("DROP TABLE IF EXISTS follows"))
        conn.commit()
        print("[OK] 旧表已删除 (topic_submissions, topics, follows)")

    # 只创建新表（social 模块的三个表）
    Follow.__table__.create(bind=engine, checkfirst=True)
    Topic.__table__.create(bind=engine, checkfirst=True)
    TopicSubmission.__table__.create(bind=engine, checkfirst=True)
    print("[OK] 新表已创建 (follows, topics, topic_submissions)")

    # 种子话题
    db = SessionLocal()
    now = datetime.now()
    topics_data = [
        Topic(
            title="用表情包表达周一的心情",
            description="周一起不来床？周一老板开会？用表情包说出你的周一故事！",
            end_time=now + timedelta(days=1),
            status=1,
        ),
        Topic(
            title="假如猫会说话",
            description="猫猫的内心OS是什么样的？用表情包帮你家的主子发声！",
            end_time=now + timedelta(days=2),
            status=1,
        ),
        Topic(
            title="当代大学生的期末状态",
            description="期末考试、论文、答辩…你的精神状态还好吗？来一张图证明你没疯！",
            end_time=now + timedelta(days=5),
            status=1,
        ),
        Topic(
            title="甲方说「再改一版」时我的反应",
            description="用表情包演绎收到反馈时的内心波澜…",
            end_time=now + timedelta(days=3),
            status=1,
        ),
        Topic(
            title="打工人的午餐图鉴",
            description="今天中午吃了啥？晒出你的打工人午餐！（注意：用表情包形式）",
            end_time=now + timedelta(days=7),
            status=1,
        ),
    ]
    db.add_all(topics_data)
    db.commit()
    print(f"[OK] 种子话题已插入 ({len(topics_data)} 条)")
    db.close()


if __name__ == "__main__":
    migrate()
