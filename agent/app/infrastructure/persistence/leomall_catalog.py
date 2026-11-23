# -*- coding: utf-8 -*-
"""LeomallProductRepository

`ProductRepository` 端口的 leomall 实现：读 `scripts/export_leomall_catalog.py`
导出的离线快照 JSON，还原成 leomall 原生 `Product` 聚合。

为什么是离线快照而不是直连 MySQL：
    - Agent 是独立 Python 服务，不该持有商城主库的写连接与账号；
    - 商品目录读多写少，快照足够，且服务启动不依赖 MySQL 可达；
    - 改价 / 上下架后重跑导出脚本并调用 `reload()`（或重启）即可刷新。
      BFF 侧仍会按 goodsId 逐条做 `isOnSale` 二次校验，快照滞后不会推下架商品。

快照缺失时不抛异常，返回空目录并在日志里给出明确的修复指引：
服务仍能启动，只是推荐为空，便于先跑通链路再补数据。
"""
from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Optional

from app.domain.catalog.ports.product_repository import ProductRepository
from app.domain.catalog.product import Product

logger = logging.getLogger(__name__)

_PRODUCT_FIELDS = (
    "product_id", "goods_sn", "name", "category_id", "brand_id", "gallery",
    "keywords", "brief", "is_on_sale", "sort_order", "pic_url", "share_url",
    "is_new", "is_hot", "unit", "counter_price", "retail_price", "detail",
    "category_name", "brand_name",
)


def load_catalog_snapshot(path: Path) -> list[Product]:
    """解析快照 JSON → Product 列表。字段按白名单过滤，兼容快照后续加字段。"""
    if not path.exists():
        logger.warning(
            "leomall 商品快照不存在：%s；请先运行 "
            "`uv run python scripts/export_leomall_catalog.py` 导出目录",
            path,
        )
        return []
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as err:
        logger.warning("leomall 商品快照解析失败（%s）：%s", path, err)
        return []

    items = raw.get("products") if isinstance(raw, dict) else raw
    if not isinstance(items, list):
        logger.warning("leomall 商品快照格式不正确，缺少 products 数组：%s", path)
        return []

    products: list[Product] = []
    for item in items:
        try:
            kwargs = {key: item[key] for key in _PRODUCT_FIELDS if key in item}
            kwargs["product_id"] = str(kwargs["product_id"])
            products.append(Product(**kwargs))
        except (KeyError, TypeError, ValueError) as err:
            logger.warning("跳过非法商品记录 %s：%s", item.get("product_id"), err)
    logger.info("leomall 商品目录已加载：%d 件在售商品（%s）", len(products), path)
    return products


class LeomallProductRepository(ProductRepository):
    def __init__(self, snapshot_path: Path, products: Optional[list[Product]] = None) -> None:
        self._snapshot_path = Path(snapshot_path)
        initial = products if products is not None else load_catalog_snapshot(self._snapshot_path)
        self._products: dict[str, Product] = {p.product_id: p for p in initial}

    @property
    def count(self) -> int:
        return len(self._products)

    def reload(self) -> int:
        """重新读取快照，返回新的商品数（供后台「重建索引」或定时任务调用）。"""
        products = load_catalog_snapshot(self._snapshot_path)
        if not products:
            logger.warning("快照为空，保留原目录不变")
            return self.count
        self._products = {p.product_id: p for p in products}
        return self.count

    async def find_by_id(self, product_id: str) -> Optional[Product]:
        return self._products.get(str(product_id))

    async def find_by_ids(self, product_ids: list[str]) -> list[Product]:
        return [
            self._products[str(pid)]
            for pid in product_ids
            if str(pid) in self._products
        ]

    async def list_all(self) -> list[Product]:
        return list(self._products.values())
