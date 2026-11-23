# -*- coding: utf-8 -*-
"""InMemoryProductRepository / InMemoryOrderRepository

`InMemoryProductRepository` 保留类名以兼容旧装配与测试，但默认数据源已切到
leomall 商品快照（`LeomallProductRepository`）。旧的跨境种子数据
`seed_products.py` 已废弃，不再参与默认装配。

OrderRepository 仍供（已停用的）跨境下单用例使用，保留仅为避免大面积删改。
leomall 集成链路不注册任何订单工具，下单走商城原生流程。
"""
from __future__ import annotations

import itertools
from typing import Optional

from app.domain.catalog.ports.product_repository import ProductRepository
from app.domain.catalog.product import Product
from app.domain.order.order import Order
from app.domain.order.ports.order_repository import OrderRepository
from app.infrastructure.persistence.leomall_catalog import (
    LeomallProductRepository,
    load_catalog_snapshot,
)
from app.infrastructure.settings import PROJECT_ROOT


class InMemoryProductRepository(ProductRepository):
    """兼容别名：默认加载 data/leomall_catalog.json。"""

    def __init__(self, products: Optional[list[Product]] = None) -> None:
        snapshot = PROJECT_ROOT / "data" / "leomall_catalog.json"
        seed = products if products is not None else load_catalog_snapshot(snapshot)
        self._products: dict[str, Product] = {p.product_id: p for p in seed}

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


class InMemoryOrderRepository(OrderRepository):
    def __init__(self) -> None:
        self._orders: dict[str, Order] = {}
        self._counter = itertools.count(1)

    async def save(self, order: Order) -> None:
        self._orders[order.order_id] = order

    async def find_by_id(self, order_id: str) -> Optional[Order]:
        return self._orders.get(order_id)

    async def next_order_id(self) -> str:
        return f"GBX-{next(self._counter):06d}"


__all__ = ["InMemoryProductRepository", "InMemoryOrderRepository", "LeomallProductRepository"]
