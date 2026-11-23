# -*- coding: utf-8 -*-
"""presentation DTO

REST 请求 / 响应模型。shopping_session_id 缺省时由服务端生成。

leomall 集成后，`/commerce/intents` 同步响应额外返回 `recommendations`：
从本轮 `product_search_tool` 的 tool.result 事件里收集的真实 goods_id 列表，
Java BFF 会逐条校验上架状态后再渲染成商品卡。
"""
from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class SubmitIntentRequest(BaseModel):
    shopping_session_id: Optional[str] = Field(default=None, description="会话 ID，缺省则新建会话")
    buyer_id: str = Field(min_length=1, description="买家 ID（由 BFF 注入，前端不可传）")
    locale: str = Field(default="zh-CN")
    currency: str = Field(default="CNY")
    raw_query: str = Field(min_length=1, description="买家自然语言购物意图")


class RecommendationItem(BaseModel):
    goods_id: int
    rank: int
    score: float = 0.0
    name: str = ""
    brief: str = ""
    pic_url: str = ""
    retail_price: float = 0.0
    counter_price: float = 0.0
    category_name: str = ""


class SubmitIntentResponse(BaseModel):
    shopping_session_id: str
    final_text: str
    recommendations: list[RecommendationItem] = Field(default_factory=list)
    model: str = ""
    degraded: bool = False  # True = 本地确定性推荐（无 LLM 密钥 / LLM 失败）
    recall_strategy: str = ""


class CancelOrderRequest(BaseModel):
    reason: str = Field(min_length=1, description="取消原因")
