# LeoMall 智能导购 Agent

Python 3.11 + FastAPI + AgentScope 2.x，为 LeoMall 提供自然语言导购与商品推荐。

## 能力

- 从 MySQL 在售商品导出离线快照 `data/leomall_catalog.json`，作为商品目录唯一事实来源
- 预算解析、意图同义词扩展、关键词召回；配置密钥后启用向量二阶段召回、RAG 品类洞察与 rerank
- 内部接口 `POST /commerce/intents` 返回 `final_text`、`recommendations[]`、`model`、`degraded`
- 服务仅监听 `127.0.0.1:8000`，通过 `X-Internal-Token` 与 Java BFF 做服务间鉴权
- 下单、查单、取消订单能力已关闭，交易统一由商城原生购物车与订单流程处理

## 配置

参考 `.env.example`，主要项：

| 变量 | 说明 |
|---|---|
| `INTERNAL_API_TOKEN` | 与后端 `leomall.agent.internal-token` 一致 |
| `LOCAL_FALLBACK` | 无 LLM 密钥时设为 1，走确定性关键词推荐 |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | OpenAI 兼容网关凭据 |
| `EMBEDDING_MODEL` / `EMBEDDING_DIM` | 向量召回配置 |
| `ENABLE_TRADE_TOOLS` | 固定为 0 |

## 密钥注入（避免暴露）

**原则：凭据只存在于进程环境变量或平台密钥服务，不落盘、不入库、不进镜像、不进浏览器。**

`settings.py` 用 `load_dotenv(PROJECT_ROOT / ".env")` 且 `override=False`，
**进程环境变量优先级高于 `.env`**，所以启用真实模型无需修改任何文件：

```bash
# Git Bash / Linux / macOS：交互式输入，避免写进 shell history
read -rs LLM_API_KEY && export LLM_API_KEY
export LOCAL_FALLBACK=0
export LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
export LLM_MODEL=<控制台确认有效的模型 ID>
uv run python -m app.presentation.server
```

PowerShell：

```powershell
$env:LLM_API_KEY = [System.Net.NetworkCredential]::new('', (Read-Host 'LLM Key' -AsSecureString)).Password
$env:LOCAL_FALLBACK = '0'
uv run python -m app.presentation.server
```

- `EMBEDDING_API_KEY` 缺省复用 `LLM_API_KEY`，不要重复配置，减少一份泄露面。
- **必须同时注入 `LOCAL_FALLBACK=0`**：`.env` 里默认为 `1`，会强制走本地关键词推荐，
  密钥有效也永远返回 `degraded=true`。
- 服务间令牌同理：`INTERNAL_API_TOKEN`（Python）与 `LEOMALL_AGENT_INTERNAL_TOKEN`（Java）
  用环境变量在两侧同时更换，且保持一致。
- 生产改用平台密钥服务挂载（Docker Compose `secrets:` / K8s Secret 只读挂载 / 云 KMS），
  不要写进 `application*.yml`、`Dockerfile`、`docker-compose.yml` 或 CI 明文变量。

**绝对禁区**：`admin/.env.*` 与 `h5/.env.*` 里的 `VUE_APP_*` 会编译进浏览器 bundle，
放进去等于公开；密钥也不能出现在 README、日志、异常响应、`/health` 或 Git 提交历史里
（一旦误提交，删文件无效，必须立即轮转密钥并清理历史）。

`/health` 只返回 `model` / `catalog_count` / `local_fallback` 等非敏感状态，不回显任何凭据。

## 启动与验证

```bash
uv sync
uv run python scripts/export_leomall_catalog.py
uv run uvicorn app.presentation.server:app --host 127.0.0.1 --port 8000
curl http://127.0.0.1:8000/health
```

`/health` 返回 `catalog_source`、`catalog_count` 与 `local_fallback` 状态。

商品改价或上下架后，需重跑导出脚本并重启服务。

## 代码结构

DDD 分层：`app/domain`（模型与端口）、`app/application`（用例、Agent、工具）、
`app/infrastructure`（持久化、向量库、LLM、配置）、`app/presentation`（FastAPI 接口）。
`app/composition.py` 为依赖装配根。
