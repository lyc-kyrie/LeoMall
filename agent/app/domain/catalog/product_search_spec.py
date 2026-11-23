# -*- coding: utf-8 -*-
"""ProductSearchSpec 值对象（leomall 版）

SearchAgent 把买家自然语言 query 改写为标准化检索规格：
normalized_query 用于召回，槽位（category / category_id / price_max_major /
is_new / is_hot）用于结构化过滤。

跨境槽位 ship_to / target_currency 已随商品目录改造一并移除：
leomall 是国内商城，价格口径恒为 CNY 元，没有目的国、运费与关税。
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class ProductSearchSpec:
    normalized_query: str
    category: Optional[str] = None
    category_id: Optional[int] = None
    locale: str = "zh-CN"
    top_k: int = 5
    # 预算硬约束（人民币元）：由检索链路结构化过滤，不交给 embedding/reranker
    price_max_major: Optional[float] = None
    price_min_major: Optional[float] = None
    is_new: Optional[bool] = None
    is_hot: Optional[bool] = None

    def __post_init__(self) -> None:
        if not self.normalized_query or not self.normalized_query.strip():
            raise ValueError("ProductSearchSpec.normalized_query required")
        if self.top_k <= 0:
            raise ValueError("ProductSearchSpec.top_k 必须为正整数")
