# -*- coding: utf-8 -*-
"""product_search_tool（leomall 版）

商品检索工具：结构化检索入参 → CatalogSearchUseCase → leomall 商品卡 JSON。
MainAgent 单干与 SearchAgent 派发两条路径共用同一工具实例。

注意：本模块不能用 `from __future__ import annotations`——
AgentScope 用 pydantic 从函数签名动态生成 JSON schema，字符串化注解会解析失败。
"""
import json
from typing import Optional

from agentscope.message import TextBlock, ToolResultState
from agentscope.tool import ToolChunk

from app.application.usecases.catalog_search import CatalogSearchUseCase
from app.domain.catalog.product_search_spec import ProductSearchSpec
from app.infrastructure.context import ShoppingContext
from app.infrastructure.eventbus import TradeEventBus


def build_product_search_tool(usecase: CatalogSearchUseCase, bus: TradeEventBus):
    async def product_search_tool(
        normalized_query: str,
        category: Optional[str] = None,
        category_id: Optional[int] = None,
        top_k: int | str = 5,
        price_max_major: float | str | None = None,
        price_min_major: float | str | None = None,
        is_new: bool = False,
        is_hot: bool = False,
    ) -> ToolChunk:
        """检索商城在售商品（embedding + rerank 二阶段召回），返回 Top-K 真实商品卡 JSON。
        商品卡含 goods_id（对应 leomall_goods.id）、name、brief、pic_url、retail_price（元）、
        counter_price、category_name、brand_name；价格口径恒为人民币元，不涉及运费与关税。
        BFF 会用 goods_id 去 leomall 校验上架状态后才展示给用户。

        Args:
            normalized_query (`str`):
                标准化检索词，保留品类词与关键属性词（如"纯棉 四件套 送礼 200元内"）。
            category (`str | None`):
                类目名称槽位，可选，如"床上用品"、"居家日用"，用于加权召回。
            category_id (`int | None`):
                leomall 类目 ID，可选；传入后作为硬约束，只返回该类目商品。
            top_k (`int`):
                返回候选数量，默认 5。
            price_max_major (`float | None`):
                价格上限（人民币元）。买家有预算硬约束时必传，由检索链路结构化过滤。
            price_min_major (`float | None`):
                价格下限（人民币元），可选。
            is_new (`bool`):
                只要新品，默认 False。
            is_hot (`bool`):
                只要人气推荐商品，默认 False。
        """
        # 模型有时会把数字参数当字符串传，这里统一强转后再进检索链路。
        if isinstance(top_k, str):
            try:
                top_k = int(top_k)
            except ValueError:
                return ToolChunk(
                    content=[TextBlock(type="text", text=f"[error] top_k 非法：{top_k}")],
                    state=ToolResultState.ERROR,
                )
        if isinstance(price_max_major, str):
            try:
                price_max_major = float(price_max_major)
            except ValueError:
                return ToolChunk(
                    content=[TextBlock(type="text", text=f"[error] price_max_major 非法：{price_max_major}")],
                    state=ToolResultState.ERROR,
                )
        if isinstance(price_min_major, str):
            try:
                price_min_major = float(price_min_major)
            except ValueError:
                return ToolChunk(
                    content=[TextBlock(type="text", text=f"[error] price_min_major 非法：{price_min_major}")],
                    state=ToolResultState.ERROR,
                )
        if isinstance(category_id, str):
            try:
                category_id = int(category_id)
            except ValueError:
                category_id = None

        session_id = ShoppingContext.current_session_id()
        args = {
            "normalized_query": normalized_query,
            "category": category,
            "category_id": category_id,
            "top_k": top_k,
            "price_max_major": price_max_major,
            "price_min_major": price_min_major,
            "is_new": is_new,
            "is_hot": is_hot,
        }
        bus.publish(session_id, "tool.invoke", {"tool": "product_search_tool", "args": args})
        try:
            spec = ProductSearchSpec(
                normalized_query=normalized_query,
                category=category,
                category_id=category_id,
                top_k=top_k,
                price_max_major=price_max_major,
                price_min_major=price_min_major,
                is_new=is_new or None,
                is_hot=is_hot or None,
            )
            result = await usecase.execute(spec)
        except ValueError as err:
            bus.publish(session_id, "tool.result", {"tool": "product_search_tool", "error": str(err)})
            return ToolChunk(
                content=[TextBlock(type="text", text=f"[error] {err}")],
                state=ToolResultState.ERROR,
            )
        bus.publish(
            session_id,
            "tool.result",
            {
                "tool": "product_search_tool",
                "hit_count": len(result["hits"]),
                "recall_strategy": result["recall_strategy"],
                "hits": result["hits"],
            },
        )
        return ToolChunk(
            content=[TextBlock(type="text", text=json.dumps(result, ensure_ascii=False))],
            state=ToolResultState.SUCCESS,
        )

    return product_search_tool
