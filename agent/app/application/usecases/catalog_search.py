# -*- coding: utf-8 -*-
"""CatalogSearchUseCase（leomall 版）

商品检索核心 UseCase，二阶段召回 + 结构化硬约束过滤：
    1. EmbeddingClient 把 normalized_query 向量化
    2. ProductVectorIndex.search(top_n) 拿候选 product_id（Qdrant，COSINE）
    3. ProductRepository.find_by_ids 还原 Product 聚合
    4. Reranker 精排；失败/未配置时降级按向量分排序（rerank_applied=false）
    5. 组装 leomall 商品卡（goods_id / name / brief / pic_url / retail_price ...）

降级链（recall_strategy 如实标注）：
    embedding_rerank → embedding_only → keyword_2gram（无 embedding 服务时兜底）

与旧版跨境实现的差异：
    - 移除 TariffSchedule / ExchangeRateTable / landed_price，价格恒为人民币元；
    - 移除 ship_to 过滤，改为 category_id / is_new / is_hot / 价格区间 / 上架状态；
    - 商品卡直接输出 leomall 的 goods_id 整数字段，BFF 无需再做字段映射。
"""
from __future__ import annotations

import logging
from typing import Optional

from app.domain.catalog.ports.product_repository import ProductRepository
from app.domain.catalog.ports.retrieval_ports import (
    EmbeddingClient,
    ProductVectorIndex,
    Reranker,
)
from app.domain.catalog.product import Product
from app.domain.catalog.product_search_spec import ProductSearchSpec

logger = logging.getLogger(__name__)

# 一阶段召回候选数（> top_k，给精排与硬约束过滤留空间）
_RECALL_TOP_N = 20

# 被硬约束挡掉的候选回传条数上限（只回摘要，避免上下文膨胀）
_FILTERED_OUT_LIMIT = 3


def tokenize(text: str) -> set[str]:
    """极简分词：空格切词 + 中文连续段落的 2-gram/3-gram（关键词降级召回用）。

    leomall 商品名是连续中文（如 "轻奢纯棉刺绣水洗四件套"），
    只靠空格切词会完全失配，必须补 n-gram。
    """
    terms: set[str] = set()
    for chunk in text.lower().split():
        # 纯数字来自预算（"200元内"）或规格（"300根"），参与召回只会制造假命中
        if chunk.isdigit():
            continue
        terms.add(chunk)
        if any("\u4e00" <= ch <= "\u9fff" for ch in chunk) and len(chunk) >= 2:
            terms.update(
                gram for gram in (chunk[i : i + 2] for i in range(len(chunk) - 1))
                if not gram.isdigit()
            )
            terms.update(
                gram for gram in (chunk[i : i + 3] for i in range(len(chunk) - 2))
                if not gram.isdigit()
            )
    return terms


class CatalogSearchUseCase:
    def __init__(
        self,
        product_repo: ProductRepository,
        embedder: Optional[EmbeddingClient] = None,
        vector_index: Optional[ProductVectorIndex] = None,
        reranker: Optional[Reranker] = None,
    ) -> None:
        self._product_repo = product_repo
        self._embedder = embedder
        self._vector_index = vector_index
        self._reranker = reranker

    async def execute(self, spec: ProductSearchSpec) -> dict:
        scored: list[tuple[float, Product]] = []
        recall_strategy = "keyword_2gram"
        rerank_applied = False

        if self._embedder is not None and self._vector_index is not None:
            try:
                scored = await self._vector_recall(spec)
                recall_strategy = "embedding_only"
            except Exception as err:  # noqa: BLE001 —— 召回基建异常必须降级而非失败
                logger.warning("向量召回不可用，降级关键词召回：%s", err)
                scored = []

        if recall_strategy == "embedding_only" and scored:
            try:
                scored = await self._rerank(spec, scored)
                recall_strategy = "embedding_rerank"
                rerank_applied = True
            except Exception as err:  # noqa: BLE001
                logger.warning("rerank 不可用，按向量分排序：%s", err)
        elif not scored:
            scored = await self._keyword_recall(spec)
            recall_strategy = "keyword_2gram"

        # 硬约束过滤 + top_k 截断（硬约束走结构化过滤，不交给模型）
        filtered: list[tuple[float, Product]] = []
        filtered_out: list[dict] = []
        for score, product in scored:
            reason = self._reject_reason(product, spec)
            if reason is None:
                filtered.append((score, product))
            elif len(filtered_out) < _FILTERED_OUT_LIMIT:
                filtered_out.append(self._to_rejected(product, reason))

        hits = [self._to_card(score, product) for score, product in filtered[: spec.top_k]]
        result = {
            "hits": hits,
            "total_candidates": len(filtered),
            "recall_strategy": recall_strategy,
            "rerank_applied": rerank_applied,
        }
        if filtered_out:
            # 如实告知"召回到了但被硬约束挡掉"，避免模型把超预算商品说成"没有这个商品"
            result["filtered_out"] = filtered_out
        return result

    def _reject_reason(self, product: Product, spec: ProductSearchSpec) -> Optional[str]:
        """返回硬约束拒绝原因，None 表示通过。"""
        if not product.is_on_sale:
            return "off_sale"
        if spec.category_id is not None and product.category_id != spec.category_id:
            return "category_mismatch"
        if spec.price_max_major is not None and product.retail_price > spec.price_max_major:
            return "over_price_cap"
        if spec.price_min_major is not None and product.retail_price < spec.price_min_major:
            return "under_price_floor"
        if spec.is_new is True and not product.is_new:
            return "not_new"
        if spec.is_hot is True and not product.is_hot:
            return "not_hot"
        return None

    @staticmethod
    def _to_rejected(product: Product, reason: str) -> dict:
        return {
            "goods_id": int(product.product_id),
            "name": product.name,
            "category_name": product.category_name,
            "retail_price": round(product.retail_price, 2),
            "currency": "CNY",
            "reason": reason,
        }

    # ---- 一阶段：向量召回 ----

    async def _vector_recall(self, spec: ProductSearchSpec) -> list[tuple[float, Product]]:
        embedding = await self._embedder.embed(spec.normalized_query)
        vector_hits = await self._vector_index.search(embedding, top_n=_RECALL_TOP_N)
        products = await self._product_repo.find_by_ids([hit.product_id for hit in vector_hits])
        by_id = {product.product_id: product for product in products}
        return [
            (hit.score, by_id[hit.product_id])
            for hit in vector_hits
            if hit.product_id in by_id
        ]

    # ---- 二阶段：精排 ----

    async def _rerank(
        self,
        spec: ProductSearchSpec,
        scored: list[tuple[float, Product]],
    ) -> list[tuple[float, Product]]:
        if self._reranker is None:
            raise RuntimeError("Reranker 未配置")
        documents = [product.searchable_text() for _, product in scored]
        rerank_scores = await self._reranker.rerank(spec.normalized_query, documents)
        reranked = [(rerank_scores[i], product) for i, (_, product) in enumerate(scored)]
        reranked.sort(key=lambda pair: pair[0], reverse=True)
        return reranked

    # ---- 兜底：关键词召回 ----

    async def _keyword_recall(self, spec: ProductSearchSpec) -> list[tuple[float, Product]]:
        query_terms = tokenize(spec.normalized_query)
        candidates: list[tuple[float, Product]] = []
        for product in await self._product_repo.list_all():
            score = self._keyword_score(query_terms, product, spec)
            if score > 0:
                candidates.append((score, product))
        candidates.sort(key=lambda pair: pair[0], reverse=True)
        return candidates

    @staticmethod
    def _keyword_score(query_terms: set[str], product: Product, spec: ProductSearchSpec) -> float:
        doc_terms = tokenize(product.searchable_text())
        matched = query_terms & doc_terms
        if not matched:
            return 0.0
        score = float(len(matched))
        # 名称命中比简介/详情命中更可信，加权
        name_terms = tokenize(product.name)
        score += 2.0 * len(query_terms & name_terms)
        # 品类槽位命中加权，让"槽位过滤"优于全文命中
        if spec.category and spec.category in product.category_name:
            score += 3.0
        if spec.category_id is not None and spec.category_id == product.category_id:
            score += 3.0
        return score

    # ---- 商品卡组装 ----

    @staticmethod
    def _to_card(score: float, product: Product) -> dict:
        card = product.to_card()
        card["score"] = round(score, 4)
        card["currency"] = "CNY"
        return card
