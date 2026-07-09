"""模仿大赛 Pydantic 模型"""

from pydantic import BaseModel, Field


class RandomTargetResponse(BaseModel):
    """随机抽取的模仿目标"""
    emoji_id: str          # label key, e.g. "单手托腮"
    label: str             # display name, e.g. "单手托腮"
    image_url: str         # emoji image path


class SubmitPhotoResponse(BaseModel):
    """提交照片打分结果"""
    match_id: int
    score: int             # 0-100
    matched_label: str     # 用户被识别到的标签
    target_label: str      # 本轮目标标签
    is_exact_match: bool   # 是否完全匹配目标标签


class MatchResultResponse(BaseModel):
    """对局结果"""
    match_id: int
    target_label: str
    target_image: str
    results: list["PlayerScore"]


class PlayerScore(BaseModel):
    user_id: int
    nickname: str
    score: int
    photo_url: str | None = None
    rank: int = 0


class LeaderboardEntry(BaseModel):
    rank: int
    user_id: int
    nickname: str
    high_score: int
    total_matches: int
    wins: int
