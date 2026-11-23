# -*- coding: utf-8 -*-
"""从 leomall MySQL 导出在售商品目录，生成 Agent 侧的商品快照 JSON。

设计取舍：
    - **零第三方依赖**：用 `mysql --xml`（MySQL 8 自带）+ 标准库 ElementTree 解析，
      不引入 pymysql / sqlalchemy 的 MySQL 驱动，Windows 上也能直接跑。
      XML 输出会正确转义中文、引号、换行与富文本里的 `<`，比 TSV/CSV 稳。
    - **离线快照**：Agent 服务不需要连 leomall 的 MySQL，改价/上下架后重跑本脚本即可。
      生产可加定时任务，或把 `LEOMALL_CATALOG_PATH` 指向挂载的共享卷。
    - **只导出在售且未删除**的商品，避免 Agent 推荐下架商品（BFF 仍会二次校验）。

用法（在 leomall-agent 根目录）：
    uv run python scripts/export_leomall_catalog.py
    # 或显式指定连接参数
    set LEOMALL_MYSQL_PASSWORD=1234 && uv run python scripts/export_leomall_catalog.py

可用环境变量：
    LEOMALL_MYSQL_BIN        mysql 可执行文件，默认 mysql
    LEOMALL_MYSQL_HOST       默认 127.0.0.1
    LEOMALL_MYSQL_PORT       默认 3306
    LEOMALL_MYSQL_USER       默认 root
    LEOMALL_MYSQL_PASSWORD   默认 1234（本机开发默认值；生产请显式注入）
    LEOMALL_MYSQL_DB         默认 leomall
    LEOMALL_CATALOG_PATH     输出路径，默认 <项目根>/data/leomall_catalog.json
"""
from __future__ import annotations

import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = PROJECT_ROOT / "data" / "leomall_catalog.json"

# 富文本详情只参与召回，保留纯文本即可，避免快照体积膨胀
_DETAIL_LIMIT = 500

_SQL = r"""
SELECT
  g.id, g.goods_sn, g.name, g.category_id, g.brand_id, g.gallery,
  g.keywords, g.brief, g.is_on_sale, g.sort_order, g.pic_url, g.share_url,
  g.is_new, g.is_hot, g.unit, g.counter_price, g.retail_price, g.detail,
  c.name AS category_name, b.name AS brand_name
FROM leomall_goods g
LEFT JOIN leomall_category c ON c.id = g.category_id AND c.deleted = 0
LEFT JOIN leomall_brand b ON b.id = g.brand_id AND b.deleted = 0
WHERE g.is_on_sale = 1 AND g.deleted = 0
ORDER BY g.id
"""


def _strip_html(raw: str) -> str:
    if not raw:
        return ""
    text = re.sub(r"<script[\s\S]*?</script>|<style[\s\S]*?</style>", " ", raw, flags=re.I)
    text = re.sub(r"<[^>]+>", " ", text)
    return re.sub(r"\s+", " ", text).strip()[:_DETAIL_LIMIT]


def _null_to_empty(value: str | None) -> str:
    return value if value is not None else ""


def _as_bool(value: str | None) -> bool:
    return str(value).strip() in ("1", "true", "True")


def _as_float(value: str | None) -> float:
    try:
        return round(float(value or 0), 2)
    except (TypeError, ValueError):
        return 0.0


def _as_int(value: str | None) -> int:
    try:
        return int(value or 0)
    except (TypeError, ValueError):
        return 0


def _gallery(value: str | None) -> list[str]:
    if not value:
        return []
    try:
        parsed = json.loads(value)
        if isinstance(parsed, list):
            return [str(item) for item in parsed if item]
    except (ValueError, TypeError):
        pass
    return []


def _run_mysql_xml() -> str:
    binary = os.getenv("LEOMALL_MYSQL_BIN", "mysql")
    host = os.getenv("LEOMALL_MYSQL_HOST", "127.0.0.1")
    port = os.getenv("LEOMALL_MYSQL_PORT", "3306")
    user = os.getenv("LEOMALL_MYSQL_USER", "root")
    password = os.getenv("LEOMALL_MYSQL_PASSWORD", "1234")
    database = os.getenv("LEOMALL_MYSQL_DB", "leomall")

    cmd = [
        binary,
        f"--host={host}",
        f"--port={port}",
        f"--user={user}",
        f"--password={password}",
        "--default-character-set=utf8mb4",
        "--xml",
        "--skip-column-names",
        "--batch",
        "-e",
        _SQL,
        database,
    ]
    completed = subprocess.run(
        cmd,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"mysql 导出失败（exit={completed.returncode}）：{completed.stderr.strip()}\n"
            "请确认 MySQL 已启动、账号密码正确、leomall 库已导入，"
            "或设置 LEOMALL_MYSQL_* 环境变量。"
        )
    return completed.stdout


def parse_rows(xml_text: str) -> list[dict]:
    """把 mysql --xml 输出解析成 [{列名: 值}]。NULL 字段带 xsi:nil，统一转空串。"""
    root = ET.fromstring(xml_text)
    rows: list[dict] = []
    for row in root.iter("row"):
        record: dict[str, str] = {}
        for field in row.iter("field"):
            name = field.get("name")
            if name:
                record[name] = _null_to_empty(field.text)
        rows.append(record)
    return rows


def build_products(rows: list[dict]) -> list[dict]:
    products: list[dict] = []
    for row in rows:
        products.append(
            {
                "product_id": str(row.get("id", "")),
                "goods_sn": row.get("goods_sn", ""),
                "name": row.get("name", ""),
                "category_id": _as_int(row.get("category_id")),
                "brand_id": _as_int(row.get("brand_id")),
                "gallery": _gallery(row.get("gallery")),
                "keywords": row.get("keywords", ""),
                "brief": row.get("brief", ""),
                "is_on_sale": _as_bool(row.get("is_on_sale", "1")),
                "sort_order": _as_int(row.get("sort_order")) or 100,
                "pic_url": row.get("pic_url", ""),
                "share_url": row.get("share_url", ""),
                "is_new": _as_bool(row.get("is_new")),
                "is_hot": _as_bool(row.get("is_hot")),
                "unit": row.get("unit") or "件",
                "counter_price": _as_float(row.get("counter_price")),
                "retail_price": _as_float(row.get("retail_price")),
                "detail": _strip_html(row.get("detail")),
                "category_name": row.get("category_name", ""),
                "brand_name": row.get("brand_name", ""),
            },
        )
    return [p for p in products if p["product_id"] and p["name"]]


def main() -> int:
    output = Path(os.getenv("LEOMALL_CATALOG_PATH", str(DEFAULT_OUTPUT)))
    output.parent.mkdir(parents=True, exist_ok=True)

    rows = parse_rows(_run_mysql_xml())
    products = build_products(rows)
    if not products:
        print("[WARN] 没有导出到任何在售商品，快照未更新（避免把空目录写进服务）", file=sys.stderr)
        return 1

    payload = {
        "source": "leomall.leomall_goods",
        "filter": "is_on_sale=1 AND deleted=0",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "count": len(products),
        "products": products,
    }
    tmp = output.with_suffix(".json.tmp")
    tmp.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    tmp.replace(output)

    categories = len({p["category_name"] for p in products if p["category_name"]})
    hot = sum(1 for p in products if p["is_hot"])
    print(
        f"[OK] 已导出 {len(products)} 个在售商品（{categories} 个类目，{hot} 个热卖）→ {output}",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
