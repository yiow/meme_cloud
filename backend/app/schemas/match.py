"""匹配相关 Pydantic 模型"""

from pydantic import BaseModel, Field


class MatchRequest(BaseModel):
    feature: list[float] = Field(..., description="特征向量（手势22维 / 表情52维）")
    mode: str = Field(default="gesture", pattern="^(gesture|expr)$")


class MatchResponse(BaseModel):
    label: str | None
    image_url: str | None
    confidence: float
    is_neutral: bool


class RecordRequest(BaseModel):
    label: str = Field(..., min_length=1)
    mode: str = Field(default="gesture", pattern="^(gesture|expr)$")
    samples: list[list[float]] = Field(..., description="特征向量列表")


class LabelItem(BaseModel):
    label: str
    images: list[str]
