"""模仿大赛服务 — 匹配、打分、排行榜"""

import json
import math
import random
from typing import Any

from app.models.game import GameMatch, GameParticipant
from app.services import matcher, mediapipe_extractor


def get_random_target() -> dict:
    """从标签库中随机抽取一个模仿目标"""
    pool = matcher.GESTURE_POOL  # 手势标签池（有明确的动作可以模仿）
    if not pool:
        # 兜底：从表情池取
        pool = matcher.EXPR_POOL
    key = random.choice(list(pool.keys()))
    images = pool[key]
    image_url = images[0] if images else ""
    # 从 LABELS 中取中文名
    label_name = key
    for item in matcher.LABELS.get("gesture", []):
        if item["key"] == key:
            label_name = item.get("label", key)
            break
    return {"emoji_id": key, "label": label_name, "image_url": image_url}


def score_imitation(feature: list[float], target_label: str) -> dict:
    """
    AI 打分：计算用户特征向量与目标标签训练样本的相似度

    算法：
    1. 从训练数据中取出目标标签的所有样本
    2. 计算用户特征与每个样本的欧氏距离，取平均值
    3. 用 KNN 分类器获取用户被识别到的标签
    4. 综合距离和标签匹配度计算 0-100 分

    返回: {"score": 0-100, "matched_label": str, "is_exact_match": bool}
    """
    # 获取目标标签的训练样本
    target_samples = []
    for label, vec in matcher._all_samples("gesture"):
        if label == target_label:
            target_samples.append(vec)

    # 用 KNN 识别用户当前做的动作
    classify_result = matcher.classify(feature, mode="gesture")
    matched_label = classify_result.get("label") or "未知"

    # 计算到目标标签的平均距离
    if target_samples and len(target_samples[0]) == len(feature):
        distances = [matcher._euclidean(feature, s) for s in target_samples]
        avg_dist = sum(distances) / len(distances)
        # 距离 → 分数转换：距离 0 → 100分，距离 threshold → 0分
        threshold = matcher.GESTURE_THRESHOLD  # 16.0
        distance_score = max(0, min(100, 100 * (1 - avg_dist / threshold)))
    else:
        avg_dist = float("inf")
        distance_score = 0

    # 标签匹配加分
    is_exact_match = (matched_label == target_label)
    if is_exact_match:
        label_bonus = 20  # 完全匹配额外加20分
    else:
        # 模糊匹配：共享字符越多越好
        overlap = len(set(matched_label) & set(target_label)) / max(len(set(target_label)), 1)
        label_bonus = int(overlap * 15)

    final_score = min(100, int(distance_score * 0.8 + label_bonus))
    final_score = max(0, final_score)

    return {
        "score": final_score,
        "matched_label": matched_label,
        "is_exact_match": is_exact_match,
        "avg_distance": round(avg_dist, 2),
    }


def generate_ai_opponents(count: int = 3) -> list[dict]:
    """生成 AI 对手的模拟分数（用于单人模式增加趣味性）"""
    opponents = []
    names = ["表情帝", "摸鱼大师", "斗图冠军", "熊猫人", "吃瓜群众", "沙雕网友",
             "接化发", "退退退", "栓Q", "摆烂王"]
    random.shuffle(names)
    for i in range(count):
        score = random.randint(30, 95)
        opponents.append({
            "user_id": -(i + 1),  # 负数表示 AI
            "nickname": names[i],
            "score": score,
            "photo_url": None,
        })
    return opponents


# ── 排行榜 ──

def get_leaderboard(limit: int = 20) -> list[dict]:
    """获取模仿大赛排行榜（按最高分降序）"""
    from app.core.database import SessionLocal
    db = SessionLocal()
    try:
        from sqlalchemy import text
        rows = db.execute(text("""
            SELECT
                gp.user_id,
                MAX(gp.score) AS high_score,
                COUNT(*) AS total_matches,
                SUM(CASE WHEN gp.score = (
                    SELECT MAX(p2.score) FROM game_participants p2
                    WHERE p2.match_id = gp.match_id
                ) THEN 1 ELSE 0 END) AS wins
            FROM game_participants gp
            GROUP BY gp.user_id
            ORDER BY high_score DESC, wins DESC
            LIMIT :limit
        """), {"limit": limit}).fetchall()

        result = []
        for i, row in enumerate(rows):
            result.append({
                "rank": i + 1,
                "user_id": row.user_id,
                "nickname": f"用户{row.user_id}",  # TODO: 关联 users 表取 nickname
                "high_score": row.high_score,
                "total_matches": row.total_matches,
                "wins": row.wins,
            })
        return result
    finally:
        db.close()
