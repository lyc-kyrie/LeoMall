# -*- coding: utf-8 -*-
"""P3-10 回归：无队列同步直跑路径的进程内幂等。"""
from __future__ import annotations

import pytest
from fastapi import HTTPException

from app.presentation import server as s


@pytest.fixture(autouse=True)
def _clean_state():
    s._sync_idem_done.clear()
    s._sync_idem_pending.clear()
    yield
    s._sync_idem_done.clear()
    s._sync_idem_pending.clear()


async def test_completed_duplicate_reuses_result():
    fp = s._intent_fingerprint("sess-a", "来一双跑步鞋")
    hit, _ = await s._sync_idem_begin(fp)
    assert not hit
    await s._sync_idem_finish(fp, {"shopping_session_id": "sess-a", "final_text": "ok"})
    hit, cached = await s._sync_idem_begin(fp)
    assert hit
    assert cached["final_text"] == "ok"


async def test_inflight_duplicate_rejected_409():
    fp = s._intent_fingerprint("sess-b", "下单")
    await s._sync_idem_begin(fp)
    with pytest.raises(HTTPException) as ei:
        await s._sync_idem_begin(fp)
    assert ei.value.status_code == 409


async def test_failure_releases_for_retry():
    fp = s._intent_fingerprint("sess-c", "q")
    await s._sync_idem_begin(fp)
    await s._sync_idem_finish(fp, None)
    hit, _ = await s._sync_idem_begin(fp)
    assert not hit


async def test_expired_result_not_reused(monkeypatch):
    fp = s._intent_fingerprint("sess-d", "q")
    await s._sync_idem_begin(fp)
    await s._sync_idem_finish(fp, {"final_text": "old"})
    # 把过期时间拨到窗口之后
    exp, data = s._sync_idem_done[fp]
    s._sync_idem_done[fp] = (exp - s._SYNC_IDEM_TTL_SECONDS - 1, data)
    hit, _ = await s._sync_idem_begin(fp)
    assert not hit


def test_fingerprint_stable_and_session_scoped():
    assert s._intent_fingerprint("s", "q") == s._intent_fingerprint("s", "q")
    assert s._intent_fingerprint("s1", "q") != s._intent_fingerprint("s2", "q")