"""
表情包匹配引擎 — 文件驱动 KNN 分类器

加载 meme-labels.json 作为标签定义，从 default.json / default_expr.json 读取训练样本。
算法与前端 index.html 一致：欧氏距离 + k=7 + 阈值 + 投票窗口。
"""

import json
import math
from collections import Counter
from pathlib import Path
from typing import Any

# ── 项目根目录（backend/../ 即 meme/）──────────────────
ROOT = Path(__file__).resolve().parent.parent.parent.parent
MEME_DIR = ROOT / "meme_match"

# ── 数据容器 ──────────────────────────────────────────
LABELS: dict[str, Any] = {}          # meme-labels.json 内容
GESTURE_POOL: dict[str, list[str]] = {}   # key → 图片路径列表（手势）
EXPR_POOL: dict[str, list[str]] = {}      # key → 图片路径列表（表情）
GESTURE_SAMPLES: list[tuple[str, list[float]]] = []  # 内置手势样本 [(label, vec), …]
EXPR_SAMPLES: list[tuple[str, list[float]]] = []
CUSTOM_GESTURE: dict[str, Any] = {}   # 用户录制的手势样本
CUSTOM_EXPR: dict[str, Any] = {}

# KNN 参数（与前端一致）
K = 7
GESTURE_THRESHOLD = 16.0    # 手势 欧氏距离阈值
EXPR_THRESHOLD = 3.0        # 表情 欧氏距离阈值


def _load_json(path: Path) -> dict:
    if not path.exists():
        return {}
    with open(path, "r", encoding="utf-8-sig") as f:
        return json.load(f)


def _save_json(path: Path, data: dict):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False)


def init_matcher():
    """启动时调用：加载所有标签和样本数据"""
    global LABELS, GESTURE_POOL, EXPR_POOL, GESTURE_SAMPLES, EXPR_SAMPLES
    global CUSTOM_GESTURE, CUSTOM_EXPR

    # 加载标签定义
    LABELS = _load_json(MEME_DIR / "meme-labels.json")

    # 手势标签池
    GESTURE_POOL.clear()
    for item in LABELS.get("gesture", []):
        key = item["key"]
        assets = item.get("assets", [])
        GESTURE_POOL[key] = [f"/static/memes/{a.split('/')[-1]}" for a in assets]

    # 表情标签池
    EXPR_POOL.clear()
    for item in LABELS.get("expression", []):
        key = item["key"]
        assets = item.get("assets", [])
        EXPR_POOL[key] = [f"/static/memes/{a.split('/')[-1]}" for a in assets]

    # 内置手势样本（gestures.json）
    GESTURE_SAMPLES = []
    gestures_data = _load_json(MEME_DIR / "gestures.json")
    for label, vectors in gestures_data.items():
        for v in vectors:
            GESTURE_SAMPLES.append((label, v))

    # 自定义手势样本（default.json）
    CUSTOM_GESTURE = _load_json(MEME_DIR / "default.json")

    # 自定义表情样本（default_expr.json）
    CUSTOM_EXPR = _load_json(MEME_DIR / "default_expr.json")


def _all_samples(mode: str) -> list[tuple[str, list[float]]]:
    """返回全部样本（内置 + 用户录制），已录制的标签覆盖内置"""
    if mode == "expr":
        overrides = set(CUSTOM_EXPR.keys())
        builtin = [(lab, v) for lab, v in EXPR_SAMPLES if lab not in overrides]
        custom = [(lab, v) for lab, d in CUSTOM_EXPR.items() for v in d.get("samples", [])]
        return builtin + custom
    else:
        overrides = set(CUSTOM_GESTURE.keys())
        builtin = [(lab, v) for lab, v in GESTURE_SAMPLES if lab not in overrides]
        custom = [(lab, v) for lab, d in CUSTOM_GESTURE.items() for v in d.get("samples", [])]
        return builtin + custom


def _euclidean(a: list[float], b: list[float]) -> float:
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)))


def classify(feature: list[float], mode: str = "gesture") -> dict:
    """
    KNN 分类

    Returns:
        {"label": str, "image_url": str | None, "confidence": float, "is_neutral": bool}
    """
    samples = _all_samples(mode)
    threshold = EXPR_THRESHOLD if mode == "expr" else GESTURE_THRESHOLD
    pool = EXPR_POOL if mode == "expr" else GESTURE_POOL

    # ── Demo 模式：无训练样本时随机返回一个标签 ──
    if not samples and pool:
        import random as _random
        demo_label = _random.choice(list(pool.keys()))
        candidates = pool.get(demo_label, [])
        return {
            "label": f"[DEMO] {demo_label}",
            "image_url": candidates[0] if candidates else None,
            "confidence": 0.99,
            "is_neutral": False,
        }

    if not samples or not feature:
        return {"label": None, "image_url": None, "confidence": 0, "is_neutral": True}

    distances = []
    for label, vec in samples:
        if len(vec) != len(feature):
            continue
        d = _euclidean(feature, vec)
        distances.append((d, label))

    # 无有效距离 → demo fallback
    if not distances:
        import random as _random2
        demo_label = _random2.choice(list(pool.keys()))
        candidates = pool.get(demo_label, [])
        return {
            "label": f"[DEMO] {demo_label}",
            "image_url": candidates[0] if candidates else None,
            "confidence": 0.99,
            "is_neutral": False,
        }

    distances.sort(key=lambda x: x[0])

    # 最近距离超过阈值 → 选 closest 而非直接 neutral
    if distances[0][0] > threshold:
        # 没有样本时返回最近的那个（比 neutral 有用）
        nearest_label = distances[0][1]
        candidates = pool.get(nearest_label, [])
        if not candidates and pool:
            import random as _random3
            nearest_label = _random3.choice(list(pool.keys()))
            candidates = pool.get(nearest_label, [])
        return {
            "label": f"≈{nearest_label}",
            "image_url": candidates[0] if candidates else None,
            "confidence": round(1.0 / (1.0 + distances[0][0]), 2),
            "is_neutral": False,
        }

    # neutral 优先判定：nearest neutral is closer than nearest gesture * 1.3
    d_neutral = next((d for d, lab in distances if lab == "neutral"), None)
    d_gesture = next((d for d, lab in distances if lab != "neutral"), None)
    if d_neutral is not None and (d_gesture is None or d_neutral <= d_gesture * 1.3):
        return {"label": None, "image_url": None, "confidence": 0, "is_neutral": True}

    # 投票（前 k 个）
    counter = Counter()
    for _, label in distances[:K]:
        counter[label] += 1

    best_label, best_count = counter.most_common(1)[0]

    # 选一张随机图片
    candidates = pool.get(best_label, [])
    # 标签不在 pool 中（如 test_gesture）→ 随机回退
    if not candidates and pool:
        import random as _random3
        best_label = _random3.choice(list(pool.keys()))
        candidates = pool.get(best_label, [])
    image_url = candidates[hash(best_label) % len(candidates)] if candidates else None

    return {
        "label": best_label,
        "image_url": image_url,
        "confidence": round(best_count / K, 2),
        "is_neutral": False,
    }


def record_sample(mode: str, label: str, samples: list[list[float]], img: str | None = None):
    """录入手势/表情样本"""
    if mode == "expr":
        if label not in CUSTOM_EXPR:
            CUSTOM_EXPR[label] = {"nombre": label, "img": img, "samples": []}
        CUSTOM_EXPR[label]["samples"].extend(samples)
        _save_json(MEME_DIR / "default_expr.json", CUSTOM_EXPR)
    else:
        if label not in CUSTOM_GESTURE:
            CUSTOM_GESTURE[label] = {"nombre": label, "img": img, "samples": []}
        CUSTOM_GESTURE[label]["samples"].extend(samples)
        _save_json(MEME_DIR / "default.json", CUSTOM_GESTURE)


def get_labels(mode: str) -> list[dict]:
    """获取所有可用标签及对应表情包"""
    pool = EXPR_POOL if mode == "expr" else GESTURE_POOL
    result = []
    for key, images in pool.items():
        result.append({"label": key, "images": images})
    return result
