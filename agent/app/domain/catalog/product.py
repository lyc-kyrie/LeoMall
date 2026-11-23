# -*- coding: utf-8 -*-
"""Product 聚合根（leomall 版）

集成 leomall 后，商品目录以 `leomall_goods` 为唯一事实来源。
本模型**完全对齐 leomall 字段**，不再有跨境 SPU / Sku / 原产国 / ships_to /
关税与到手价概念：一个 Product 就是 leomall 里一件可直接下单的商品。

字段来源：`leomall-db/sql/leomall_table.sql` 的 `leomall_goods` 建表语句，
以及 `leomall_category` / `leomall_brand` 的名称冗余（便于召回与商品卡渲染）。

`product_id` 保留 str 是为了不破坏 `ProductRepository` / `ProductVectorIndex` 端口契约；
它由 BFF 转成整数 goodsId 后再交给 leomall 校验。
"""
from __future__ import annotations

import re
from dataclasses import dataclass, field


@dataclass
class Product:
    # ---- leomall_goods 原生字段 ----
    product_id: str
    goods_sn: str = ""
    name: str = ""
    category_id: int = 0
    brand_id: int = 0
    gallery: list[str] = field(default_factory=list)
    keywords: str = ""
    brief: str = ""
    is_on_sale: bool = True
    sort_order: int = 100
    pic_url: str = ""
    share_url: str = ""
    is_new: bool = False
    is_hot: bool = False
    unit: str = "件"
    counter_price: float = 0.0
    retail_price: float = 0.0
    detail: str = ""
    # ---- 冗余的名称字段（来自 leomall_category / leomall_brand，非 goods 表列）----
    category_name: str = ""
    brand_name: str = ""

    def __post_init__(self) -> None:
        if not self.product_id:
            raise ValueError("Product.product_id required")
        if not self.name:
            raise ValueError(f"Product.name required：{self.product_id}")

    # 兼容检索链路里少量遗留读法；新代码请直接用 name / category_name / brand_name。
    @property
    def title(self) -> str:
        return self.name

    @property
    def category(self) -> str:
        return self.category_name

    @property
    def brand(self) -> str:
        return self.brand_name

    def searchable_text(self) -> str:
        """召回用可检索文本：名称 + 类目 + 品牌 + 关键字 + 简介 + 纯文本详情。

        leomall 的 keywords 多为逗号分隔、detail 是富文本，这里做一次轻量清洗，
        避免 HTML 标签把 embedding 与关键词召回带偏。
        """
        keyword_text = self.keywords.replace(",", " ").replace("，", " ").strip()
        detail_text = _strip_html(self.detail)[:300]
        parts = [
            self.name,
            self.category_name,
            self.brand_name,
            keyword_text,
            self.brief,
            detail_text,
            "新品" if self.is_new else "",
            "热卖" if self.is_hot else "",
        ]
        return " ".join(part for part in parts if part)

    def to_card(self) -> dict:
        """商品卡（内部 Agent 视角）。goods_id 用整数，供 BFF 直接校验。"""
        return {
            "goods_id": int(self.product_id),
            "goods_sn": self.goods_sn,
            "name": self.name,
            "category_id": self.category_id,
            "category_name": self.category_name,
            "brand_id": self.brand_id,
            "brand_name": self.brand_name,
            "brief": self.brief,
            "pic_url": self.pic_url,
            "retail_price": round(self.retail_price, 2),
            "counter_price": round(self.counter_price, 2),
            "unit": self.unit,
            "is_new": self.is_new,
            "is_hot": self.is_hot,
        }


def _strip_html(raw: str) -> str:
    """极简 HTML 去标签，够用即可（detail 只参与召回，不对外展示）。"""
    if not raw:
        return ""
    text = re.sub(r"<script[\s\S]*?</script>|<style[\s\S]*?</style>", " ", raw, flags=re.I)
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", text).strip()
