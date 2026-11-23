# -*- coding: utf-8 -*-
"""seed_products（已废弃，仅保留导入兼容）

商品目录改造后，leomall 是唯一事实来源：
    - 旧版 39 个跨境 SPU（P1001~P1039，含 Sku / origin_country / ships_to /
      关税到手价）已全部移除；
    - 现在由 `scripts/export_leomall_catalog.py` 从 leomall MySQL 导出
      238 个真实在售商品到 `data/leomall_catalog.json`；
    - `LeomallProductRepository` 负责加载，`InMemoryProductRepository` 只留兼容别名。

本模块保留 `build_seed_products()` 名字，让仍 import 它的旧测试/脚本不会在
收集阶段炸掉，但返回的是 leomall 商品而非旧的跨境种子。
依赖 P1001 等旧 ID 与排序的断言属于已废弃契约，需按新目录重写。
"""
from __future__ import annotations

from app.domain.catalog.product import Product
from app.infrastructure.persistence.leomall_catalog import load_catalog_snapshot
from app.infrastructure.settings import PROJECT_ROOT


def build_seed_products() -> list[Product]:
    """兼容入口：返回 leomall 快照里的在售商品。"""
    return load_catalog_snapshot(PROJECT_ROOT / "data" / "leomall_catalog.json")
