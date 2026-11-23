# -*- coding: utf-8 -*-
"""FastAPI 服务入口

路由：
    POST /commerce/intents                 提交买家意图（同步返回最终回复；启用队列时内部入队后等结果）
    POST /commerce/intents/async           提交买家意图（立即返回 task_id，结果走 WS 或轮询）
    GET  /commerce/tasks/{task_id}         查任务状态（queued / running / done / failed）
    WS   /commerce/events                  订阅会话事件流
    GET  /commerce/orders/{order_id}       查询订单（直连 UseCase，不过 Agent）
    POST /commerce/orders/{order_id}/cancel  取消订单（直连 UseCase）
    GET  /health                           健康检查（含依赖连通性与队列深度）

启动：
    uv run uvicorn app.presentation.server:app --port 8000
    uv run python -m app.worker          # 启用队列时另起消费进程

同步接口为什么保留：13 case 评测脚本与前端都依赖它直接返回 final_text，
改成纯异步会一次性搞挂回归与前端。削峰由 worker 并发度保证，与接口形态无关。
"""
from __future__ import annotations

import asyncio
import contextlib
import hashlib
import logging
import re
import time
import uuid
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, Header, HTTPException, WebSocket
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app.application.agents.orchestrator import SubmitIntentInput
from app.composition import Container, build_container
from app.domain.catalog.product_search_spec import ProductSearchSpec
from app.domain.queue.ports.task_queue import IntentTask, TaskStatus
from app.presentation.connection import ConnectionManager
from app.presentation.dto import (
    CancelOrderRequest,
    RecommendationItem,
    SubmitIntentRequest,
    SubmitIntentResponse,
)

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")

logger = logging.getLogger(__name__)

# 幂等键有效期：同一会话同一句话在此窗口内重复提交视为重复请求
_IDEMPOTENCY_TTL_SECONDS = 600
# 轮数计数器存活时长：比幂等窗口长得多，让一整段会话都能被正确分类
_TURN_COUNTER_TTL_SECONDS = 86400


# ---- leomall 集成：内部鉴权 / 推荐结果提取 / 本地确定性降级 ----

_BUDGET_PATTERNS = (
    r"(?:预算|不超过|不高于|低于|少于|控制在)\s*(\d+(?:\.\d+)?)\s*(?:元|块|块钱|rmb)?",
    r"(\d+(?:\.\d+)?)\s*(?:元|块|块钱)\s*(?:以内|以下|之内|内|左右|预算)",
    r"(\d+(?:\.\d+)?)\s*(?:元|块|块钱)\s*(?:能买|可以买|搞定|左右)",
)


def _parse_budget(raw_query: str) -> Optional[float]:
    """从自然语言里抽出预算上限（人民币元），抽不到返回 None。"""
    for pattern in _BUDGET_PATTERNS:
        match = re.search(pattern, raw_query, flags=re.I)
        if match:
            try:
                value = float(match.group(1))
                if 0 < value < 1_000_000:
                    return value
            except ValueError:
                continue
    return None


# 本地模式下没有 LLM 做 query 改写，用一张极简意图同义词表补足口语 → 商品名鸿沟
_QUERY_SYNONYMS = (
    ("礼物", "礼盒 礼品 送礼 伴手礼"),
    ("送人", "礼盒 礼品 送礼"),
    ("送礼", "礼盒 礼品"),
    ("床品", "四件套 被 枕 床单"),
    ("睡觉", "四件套 被 枕 床品"),
    ("养猫", "猫 宠物"),
    ("养狗", "狗 宠物"),
    ("做饭", "锅 厨具 厨房"),
    ("厨房", "锅 厨具"),
    ("收纳", "收纳 整理 置物"),
)


def _expand_query(raw_query: str) -> str:
    """去掉预算短语，补上常见购物意图的同义词，供关键词降级召回使用。"""
    cleaned = re.sub(
        r"(?:预算|不超过|不高于|低于|少于|控制在)?\s*\d+(?:\.\d+)?\s*(?:元|块|块钱)?\s*"
        r"(?:以内|以下|之内|内|左右|能买|可以买|搞定)?",
        " ",
        raw_query,
    )
    extras = [words for keyword, words in _QUERY_SYNONYMS if keyword in raw_query]
    return " ".join([cleaned.strip(), *extras]).strip() or raw_query


def _to_recommendations(hits: list[dict]) -> list[RecommendationItem]:
    """把 tool.result 里的商品卡收敛成去重、按分数排序的推荐列表。"""
    best: dict[int, dict] = {}
    for hit in hits:
        try:
            goods_id = int(hit["goods_id"])
        except (KeyError, TypeError, ValueError):
            continue
        current = best.get(goods_id)
        if current is None or float(hit.get("score") or 0) > float(current.get("score") or 0):
            best[goods_id] = hit
    ordered = sorted(best.values(), key=lambda h: float(h.get("score") or 0), reverse=True)
    return [
        RecommendationItem(
            goods_id=int(h["goods_id"]),
            rank=index + 1,
            score=round(float(h.get("score") or 0), 4),
            name=str(h.get("name") or ""),
            brief=str(h.get("brief") or ""),
            pic_url=str(h.get("pic_url") or ""),
            retail_price=float(h.get("retail_price") or 0),
            counter_price=float(h.get("counter_price") or 0),
            category_name=str(h.get("category_name") or ""),
        )
        for index, h in enumerate(ordered)
    ]


async def _collect_hits(c: Container, session_id: str, awaitable) -> tuple[str, list[dict]]:
    """跑一轮 Agent，同时收集本轮 product_search_tool 产生的商品卡事件。"""
    queue = c.bus.subscribe(session_id)
    try:
        result = await awaitable
        final_text = result.final_text if hasattr(result, "final_text") else str(result)
        hits: list[dict] = []
        while not queue.empty():
            event = queue.get_nowait()
            payload = event.payload if hasattr(event, "payload") else None
            if (
                getattr(event, "type", "") == "tool.result"
                and isinstance(payload, dict)
                and payload.get("tool") == "product_search_tool"
                and isinstance(payload.get("hits"), list)
            ):
                hits.extend(payload["hits"])
        return final_text, hits
    finally:
        c.bus.unsubscribe(session_id, queue)


async def _local_recommend(c: Container, raw_query: str) -> tuple[str, list[dict], str]:
    """无 LLM 密钥时的确定性推荐：直接跑检索链路 + 结构化预算过滤。

    保证 leomall 集成在零外部密钥条件下也能端到端跑通与验收。
    正式环境配置 LLM_API_KEY 后会自动走完整 Agent 链路。
    """
    budget = _parse_budget(raw_query)
    spec = ProductSearchSpec(
        normalized_query=_expand_query(raw_query),
        top_k=5,
        price_max_major=budget,
    )
    result = await c.catalog_search.execute(spec)
    hits: list[dict] = list(result["hits"])

    # 关键词完全没命中时给热卖/新品，避免用户看到空回复
    if not hits:
        products = await c.product_repo.list_all()
        pool = [
            p for p in products
            if p.is_on_sale and (budget is None or p.retail_price <= budget)
        ]
        pool.sort(key=lambda p: (p.is_hot, p.is_new, -p.sort_order), reverse=True)
        hits = [{**p.to_card(), "score": 0.0} for p in pool[: spec.top_k]]

    if hits:
        names = "、".join(str(h.get("name") or "") for h in hits[:3])
        budget_text = f"（预算 {budget:g} 元内）" if budget else ""
        text = (
            f"根据「{raw_query}」{budget_text}，从商城在售商品里为你挑了 {len(hits)} 件：{names}。"
            "点下方商品卡可以看详情、加入购物车。"
        )
    else:
        text = (
            f"暂时没有找到符合「{raw_query}」的在售商品，"
            "换个说法或放宽预算再试试吧。"
        )
    return text, hits, result["recall_strategy"]


def build_app() -> FastAPI:
    state: dict = {}

    async def _forward_remote_events(c: Container) -> None:
        """把其他进程（worker）广播的事件转发给本进程的 WS 订阅者。"""
        if c.backplane is None:
            return
        try:
            async for event in c.backplane.listen():
                c.bus.deliver_local(event)
        except asyncio.CancelledError:
            raise
        except Exception as err:  # noqa: BLE001
            logger.warning("事件背板监听中断：%s", err)

    @asynccontextmanager
    async def lifespan(_: FastAPI):
        c = await build_container()
        state["c"] = c
        state["connections"] = ConnectionManager(c.bus)
        await c.startup()
        if c.backplane is not None:
            # 跨进程事件转发：不开这个任务，worker 产生的流式事件到不了前端
            state["forwarder"] = asyncio.create_task(_forward_remote_events(c))
        try:
            yield
        finally:
            forwarder = state.pop("forwarder", None)
            if forwarder is not None:
                forwarder.cancel()
                with contextlib.suppress(asyncio.CancelledError):
                    await forwarder
            state.pop("c", None)
            await c.shutdown()

    api = FastAPI(title="leomall 智能导购 Agent", version="0.5.0", lifespan=lifespan)

    def container() -> Container:
        if "c" not in state:
            raise HTTPException(status_code=503, detail="服务尚未就绪")
        return state["c"]

    settings_origins = build_container_origins()
    api.add_middleware(
        CORSMiddleware,
        allow_origins=settings_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @api.get("/health")
    async def health() -> dict:
        """依赖连通性一并报出，避免"进程活着但存储已挂"被当成健康。"""
        c = container()
        database = "disabled"
        if c.db_engine is not None:
            try:
                async with c.db_engine.connect() as conn:
                    await conn.execute(text("select 1"))
                database = c.db_engine.url.get_backend_name()
            except Exception as err:  # noqa: BLE001
                # 异常原文可能含 DSN / 凭据，只写日志，不回显到 /health
                logger.warning("health check database error: %s", err)
                database = "error"
        redis_state = "disabled"
        if c.cache.enabled:
            redis_state = "ok" if await c.cache.ping() else "error"
        return {
            "status": "ok",
            "model": c.settings.llm_model,
            "database": database,
            "redis": redis_state,
            "semantic_cache": c.semantic_cache.enabled,
            "queue": "enabled" if c.task_queue is not None else "disabled",
            "queue_depth": await c.task_queue.depth() if c.task_queue is not None else 0,
            # leomall 集成可观测项
            "catalog_source": "leomall_snapshot",
            "catalog_count": c.product_repo.count,
            "local_fallback": c.settings.local_fallback,
        }

    @api.post("/commerce/intents", response_model=SubmitIntentResponse)
    async def submit_intent(
        body: SubmitIntentRequest,
        x_internal_token: Optional[str] = Header(default=None, alias="X-Internal-Token"),
    ) -> SubmitIntentResponse:
        c = container()

        # 内部服务鉴权：BFF 才持有共享密钥；服务本身只监听 127.0.0.1。
        # buyer_id 由 BFF 从登录态注入，前端无法伪造；密钥为空时仅限本机开发。
        expected = c.settings.internal_api_token
        if expected and x_internal_token != expected:
            raise HTTPException(status_code=401, detail="invalid internal token")

        session_id = body.shopping_session_id or f"session-{uuid.uuid4().hex[:8]}"

        # 无 LLM 密钥（或显式 LOCAL_FALLBACK=1）时走确定性推荐，保证链路可用
        if c.settings.local_fallback:
            final_text, hits, strategy = await _local_recommend(c, body.raw_query)
            return SubmitIntentResponse(
                shopping_session_id=session_id,
                final_text=final_text,
                recommendations=_to_recommendations(hits),
                model="local-fallback",
                degraded=True,
                recall_strategy=strategy,
            )

        intent = SubmitIntentInput(
            shopping_session_id=session_id,
            buyer_id=body.buyer_id,
            locale=body.locale,
            currency=body.currency,
            raw_query=body.raw_query,
        )
        if c.task_queue is None:
            final_text, hits = await _collect_hits(
                c, session_id, c.orchestrator.handle_intent(intent),
            )
            return SubmitIntentResponse(
                shopping_session_id=session_id,
                final_text=final_text,
                recommendations=_to_recommendations(hits),
                model=c.settings.llm_model,
                degraded=False,
            )

        task_id = await _enqueue(c, intent)
        final_text = await _await_result(c, task_id, session_id)
        # 队列模式下 worker 在别的进程，事件经 backplane 转发；这里不重复订阅，
        # 推荐结果由 BFF 侧按 reply 中的商品名兜底展示（P0 默认不启用 Redis 队列）。
        return SubmitIntentResponse(
            shopping_session_id=session_id,
            final_text=final_text,
            model=c.settings.llm_model,
        )

    @api.post("/commerce/intents/async")
    async def submit_intent_async(body: SubmitIntentRequest) -> dict:
        c = container()
        session_id = body.shopping_session_id or f"session-{uuid.uuid4().hex[:8]}"
        intent = SubmitIntentInput(
            shopping_session_id=session_id,
            buyer_id=body.buyer_id,
            locale=body.locale,
            currency=body.currency,
            raw_query=body.raw_query,
        )
        if c.task_queue is None:
            raise HTTPException(status_code=503, detail="队列未启用，请使用 /commerce/intents")
        task_id = await _enqueue(c, intent)
        return {"shopping_session_id": session_id, "task_id": task_id, "state": "queued"}

    @api.get("/commerce/tasks/{task_id}")
    async def get_task(task_id: str) -> dict:
        c = container()
        if c.task_queue is None:
            raise HTTPException(status_code=503, detail="队列未启用")
        status = await c.task_queue.get_status(task_id)
        if status is None:
            raise HTTPException(status_code=404, detail=f"任务不存在或已过期：{task_id}")
        return {
            "task_id": status.task_id,
            "state": status.state,
            "final_text": status.final_text,
            "error": status.error,
            "queue_position": status.queue_position,
        }

    @api.websocket("/commerce/events")
    async def commerce_events(websocket: WebSocket) -> None:
        await state["connections"].serve(websocket)

    @api.get("/commerce/orders/{order_id}")
    async def get_order(order_id: str) -> dict:
        try:
            return await container().query_order.execute(order_id)
        except ValueError as err:
            raise HTTPException(status_code=404, detail=str(err)) from err

    @api.post("/commerce/orders/{order_id}/cancel")
    async def cancel_order_endpoint(order_id: str, body: CancelOrderRequest) -> dict:
        try:
            return await container().cancel_order.execute(order_id, body.reason)
        except ValueError as err:
            raise HTTPException(status_code=400, detail=str(err)) from err

    return api


async def _queue_priority(c: Container, session_id: str) -> int:
    """按对话轮数定队列优先级（0 = 正常，1 = 大请求）。

    长会话上下文大、单次耗时长，分到低优先流，避免堵住新会话。
    轮数计数存 Redis（计数不原子，但优先级本身是启发式，差一两次无影响）；
    Redis 不可用或开关关闭时一律返回 0，退化为单队列。
    """
    if not c.settings.queue_priority_enabled or not c.cache.enabled:
        return 0
    key = f"leomall:turns:{session_id}"
    try:
        current = int(await c.cache.get_raw(key) or 0) + 1
        await c.cache.set_json(key, current, _TURN_COUNTER_TTL_SECONDS)
    except Exception:  # noqa: BLE001 —— 计数失败不能影响入队
        return 0
    return 1 if current >= c.settings.queue_large_request_turns else 0


async def _enqueue(c: Container, intent: SubmitIntentInput) -> str:
    """入队并做幂等保护。

    队列是 at-least-once，且买家/前端可能重复提交。用「会话 + 问句」指纹做幂等键，
    命中说明短时间内已提交过同样内容，直接复用原 task_id，不再入队一次。
    这一步对写操作（下单）尤其关键：重复消费等于重复下单。
    """
    fingerprint = hashlib.sha256(
        f"{intent.shopping_session_id}\n{intent.raw_query}".encode(),
    ).hexdigest()[:32]
    idem_key = f"idem:{fingerprint}"
    task_id = f"task-{uuid.uuid4().hex[:12]}"

    acquired = await c.cache.set_if_absent(idem_key, task_id, _IDEMPOTENCY_TTL_SECONDS)
    if not acquired:
        previous = await c.cache.get_raw(idem_key)
        if previous:
            logger.info("幂等命中，复用已有任务：%s", previous)
            return previous

    await c.task_queue.enqueue(  # type: ignore[union-attr]
        IntentTask(
            task_id=task_id,
            shopping_session_id=intent.shopping_session_id,
            buyer_id=intent.buyer_id,
            locale=intent.locale,
            currency=intent.currency,
            raw_query=intent.raw_query,
            priority=await _queue_priority(c, intent.shopping_session_id),
        ),
    )
    await c.task_queue.set_status(TaskStatus(task_id=task_id, state="queued"))  # type: ignore[union-attr]
    c.bus.publish(intent.shopping_session_id, "task.queued", {"task_id": task_id})
    return task_id


async def _await_result(c: Container, task_id: str, session_id: str) -> str:
    """等 worker 跑完。

    优先等 final.result 事件（实时）；同时定期查任务状态兜底——
    worker 崩溃或任务进死信时事件永远不会来，只靠等事件会把请求挂死。
    """
    queue = c.bus.subscribe(session_id)
    deadline = time.monotonic() + c.settings.queue_wait_seconds
    try:
        while time.monotonic() < deadline:
            try:
                event = await asyncio.wait_for(queue.get(), timeout=2.0)
            except asyncio.TimeoutError:
                status = await c.task_queue.get_status(task_id)  # type: ignore[union-attr]
                if status is not None and status.state == "done":
                    return status.final_text
                if status is not None and status.state == "failed":
                    return f"[error] {status.error}"
                continue
            if event.type == "final.result":
                return str(event.payload.get("text", ""))
        return "[error] 处理超时，请稍后重试或改用异步接口查询任务状态"
    finally:
        c.bus.unsubscribe(session_id, queue)


def build_container_origins() -> list[str]:
    """CORS 需要在 app 构造期就确定，此处单独读一次配置。"""
    from app.infrastructure.settings import load_settings

    return load_settings().cors_origins


app = build_app()


if __name__ == "__main__":
    import uvicorn

    from app.infrastructure.settings import load_settings

    _s = load_settings()
    uvicorn.run(app, host=_s.host, port=_s.port)
