# -*- coding: utf-8 -*-
"""ConnectionManager

WebSocket 连接管理：客户端连上 /commerce/events 后发送
{"shopping_session_id": "...", "internal_token": "..."} 完成订阅，
服务端把 TradeEventBus 中对应会话的事件推送给该连接。

P0-6：订阅必须携带内部 token（配置了 INTERNAL_API_TOKEN 时强制校验），
防止任意连接窃听他人会话事件流。
"""
from __future__ import annotations

import asyncio
import logging
from dataclasses import asdict
from typing import Callable, Optional

from fastapi import WebSocket, WebSocketDisconnect

from app.infrastructure.eventbus import TradeEventBus

logger = logging.getLogger(__name__)


class ConnectionManager:
    def __init__(
        self,
        bus: TradeEventBus,
        internal_token_getter: Optional[Callable[[], str]] = None,
    ) -> None:
        self._bus = bus
        self._internal_token_getter = internal_token_getter or (lambda: "")

    async def serve(self, websocket: WebSocket) -> None:
        await websocket.accept()
        try:
            subscribe_payload = await websocket.receive_json()
        except WebSocketDisconnect:
            return
        # P0-6：内部 token 校验（与 HTTP 端点同一密钥）
        expected = self._internal_token_getter()
        if expected and subscribe_payload.get("internal_token") != expected:
            await websocket.close(code=4001, reason="内部 token 无效")
            return
        session_id = subscribe_payload.get("shopping_session_id")
        if not session_id:
            await websocket.close(code=4000, reason="缺少 shopping_session_id")
            return

        queue = self._bus.subscribe(session_id)
        logger.info("WebSocket 已订阅会话：%s", session_id)
        try:
            while True:
                event = await queue.get()
                payload = asdict(event)
                payload.pop("shopping_session_id", None)
                await websocket.send_json(payload)
        except (WebSocketDisconnect, asyncio.CancelledError):
            pass
        finally:
            self._bus.unsubscribe(session_id, queue)
            logger.info("WebSocket 已退订会话：%s", session_id)
