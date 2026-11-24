# LeoMall

一体化智能商城系统：Spring Boot 多模块后端 + Vue2 管理后台 + Vue2 H5 用户端 + Python 智能导购 Agent。

## 目录结构

| 目录 | 内容 |
|---|---|
| `server/` | Java 后端多模块（`com.leomall.*`），父 POM 在 `server/pom.xml` |
| `admin/` | 管理后台前端（Vue2 + Element UI），含智能助手会话记录与推荐效果 |
| `h5/` | 用户端 H5（Vue2 + Vant），含全局悬浮智能导购对话框 |
| `agent/` | 智能导购 Agent（Python 3.11 + FastAPI + AgentScope 2.x） |
| `sql/` | 数据库建库 / 建表 / 初始数据脚本 |
| `deploy/` | 单机与容器部署配置样例 |
| `miniprogram/` | 小程序端源码（当前主推 H5，此目录为保留代码） |
| `docs/` | 架构与启动说明 |

## 技术栈

- 后端：Spring Boot 2.7.18 / Java 8（编译目标，JDK 17 可运行）/ Maven / MyBatis / Shiro / Druid / MySQL 8
  （阶段2A 已从 2.1.5 跳版；下一步阶段2B：Java 17 + Boot 3.x + Spring Security）
- 前端：Vue2 + vue-cli 3（webpack4），管理后台 Element UI 2.x，H5 Vant2
- Agent：Python 3.11、FastAPI、AgentScope 2.x、Qdrant（可选）、SQLite

## 快速启动

### 1. 数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/leomall_schema.sql
mysql -uroot -p --default-character-set=utf8mb4 leomall < sql/leomall_table.sql
mysql -uroot -p --default-character-set=utf8mb4 leomall < sql/leomall_data.sql
```

建库脚本创建独立数据库 `leomall` 与同名账户，不影响其他数据库。账户口令见 `sql/leomall_schema.sql`，并已与 `server/leomall-db/src/main/resources/application-db.yml` 的默认值对齐（仅本机开发用）；生产通过 `LEOMALL_MYSQL_URL` / `LEOMALL_MYSQL_USER` / `LEOMALL_MYSQL_PASSWORD` 环境变量注入，并同步修改建库脚本。

### 2. 后端

**启动前必须配置以下环境变量**（P0 安全修复后为必填项）：

| 环境变量 | 说明 |
|---|---|
| `LEOMALL_JWT_SECRET` | 用户端 JWT 签名密钥，≥32 字符强随机串（可用 `openssl rand -hex 32` 生成）。**未配置后端拒绝启动**；更换后所有已登录用户 token 失效，需重新登录 |
| `LEOMALL_AGENT_INTERNAL_TOKEN` | Java BFF 与导购 Agent 的共享令牌，必须与 `agent/.env` 的 `INTERNAL_API_TOKEN` 一致；未配置时导购聊天不可用 |

按需配置（P1 修复后新增，不用对应功能可留空）：

| 环境变量 | 说明 |
|---|---|
| `LEOMALL_WX_APP_SECRET` | 微信小程序 app-secret（微信登录/支付必填；原演示值已从仓库移除） |
| `LEOMALL_COS_SECRET_ID` / `LEOMALL_COS_SECRET_KEY` | 腾讯云 COS 密钥（`storage.active=tencent` 时必填） |
| `LEOMALL_MYSQL_URL` / `LEOMALL_MYSQL_USER` / `LEOMALL_MYSQL_PASSWORD` | 覆盖数据库连接（默认值仅本机开发） |
| `LEOMALL_CORS_ALLOWED_ORIGINS` | 跨域来源白名单（逗号分隔）。**默认空=不开放跨域**；前端同源代理/nginx 同源部署无需配置 |
| `LEOMALL_REDIS_ENABLED` | 阶段1：`true`=共享状态走 Redis（多实例必需）；默认 `false`=单实例内存态，本机开发无需 Redis |
| `LEOMALL_REDIS_HOST` / `LEOMALL_REDIS_PORT` / `LEOMALL_REDIS_PASSWORD` / `LEOMALL_REDIS_DB` | Redis 连接参数（仅 enabled=true 时使用，默认 localhost:6379/0） |
| `SWAGGER_ENABLED` | 阶段2A：在线文档开关（springdoc），默认 `true`；**生产设 `false`** 整体关闭 `/v3/api-docs` 与 swagger-ui（替代原 `swagger.production`） |

PowerShell 示例：

```powershell
$env:LEOMALL_JWT_SECRET = "<32位以上强随机串>"
$env:LEOMALL_AGENT_INTERNAL_TOKEN = "<与 agent/.env 中 INTERNAL_API_TOKEN 一致>"
```

```bash
cd server
mvn clean package -DskipTests
java -jar leomall-all/target/leomall-all-0.1.0-exec.jar --server.port=8080
```

需 JDK 8+（编译目标 1.8，本机 JDK 17 亦可直接运行）；若环境变量存在 `SERVER__PORT`，启动前先 `unset SERVER__PORT`。

### 3. 前端

```bash
cd admin && npm install && npm run dev    # 9527
cd h5    && npm install && npm run dev    # 6255
```

Node 18+ 运行 webpack4 需设置 `NODE_OPTIONS=--openssl-legacy-provider`。

### 4. 智能导购 Agent

```bash
cd agent
uv sync
LEOMALL_MYSQL_PASSWORD=<密码> uv run python scripts/export_leomall_catalog.py
uv run uvicorn app.presentation.server:app --host 127.0.0.1 --port 8000
```

`agent/.env` 的 `INTERNAL_API_TOKEN` 需与后端环境变量 `LEOMALL_AGENT_INTERNAL_TOKEN` 保持一致
（`application-wx.yml` 已不再提供任何默认令牌）。未配置 `LLM_API_KEY` 时自动进入本地确定性推荐模式。

**服务凭据不要写进任何文件**：`LLM_API_KEY` 等通过进程环境变量注入（环境变量优先级高于 `.env`），
生产用平台密钥服务挂载；完整做法见 `agent/README.md` 的「密钥注入」章节。

初始账号由 `sql/leomall_schema.sql` 种子数据写入（管理后台管理员与 H5 演示用户），
管理员**首次登录强制修改密码**。种子口令仅供本地开发：部署到任何共享/生产环境前，
必须修改演示用户口令或删除演示账号，切勿沿用种子口令。

安全说明（P0/P1/P2 修复后生效，详见 `docs/AUDIT.md` 第五、六、七节）：

- 管理后台登录需**图形验证码**；同一账号连续失败 5 次锁定 15 分钟。
- 用户端文件上传**必须登录**，仅允许 jpg/jpeg/png/gif/webp/bmp 图片；非图片访问一律附件下载。
- 优惠券核销校验归属与可用状态，同一张券不可重复使用；领取/兑换有串行锁防并发突破限领。
- Agent 全部 HTTP/WS 端点校验内部 token；订单查询/取消校验买家归属；开发编排端口仅映射到宿主机回环。
- 入口对登录/验证码/上传/导购聊天限流（超限 429）；下单 10 秒窗口防重复提交；未支付订单有定时兜底取消。
- 商品详情/专题/公告富文本入库前服务端消毒；管理员接口响应不含密码哈希。
- 数据库备份校验成败；应用 DB 账号不再持有 DDL 权限（**存量数据库需手动重跑 `sql/leomall_schema.sql` 中的 grant 语句生效**）。
- CORS 默认不开放跨域（白名单可配）；管理端会话 Cookie HttpOnly+SameSite=Lax。
- 下单校验购物车归属，并复核商品现价与在售状态（价格变化拒绝下单）。
- 短信验证码：号码日限 10 条 + IP 小时限 20 条，一次性使用，连续错 5 次作废。
- 微信自动建号使用随机不可登录密码（**存量库中历史明文 openId 密码需一次性迁移重置**）。
- 导购聊天每用户约 6 次/分钟、每日 100 次、并发 ≤2；Agent 网关全局在途 ≤20，连续失败自动熔断 30 秒。

## 阶段1：Redis 共享状态（可选，多实例必需）

默认情况下，短信验证码、管理端登录锁定、下单去重、领券锁、导购聊天限流、管理端会话均为**单实例内存实现**，本机开发零 Redis 依赖。设置 `LEOMALL_REDIS_ENABLED=true` 后全部切换为 Redis 共享存储（StateStore 抽象，原子计数/SETNX/Lua 释放锁），支持后端水平扩展：

```powershell
# 本机试用：起一个临时 Redis
docker run -d --name leomall-redis -p 127.0.0.1:6379:6379 redis:7-alpine
$env:LEOMALL_REDIS_ENABLED = "true"
```

云端部署（`deploy/cloud`）：

```bash
# .env 设 LEOMALL_REDIS_ENABLED=true、REDIS_URL=redis://redis:6379/0
docker compose --profile cache up -d --build
# 再启用 Agent 队列削峰（.env 设 QUEUE_ENABLED=1，含 worker 消费进程）
docker compose --profile cache --profile queue up -d --build
```

说明：

- 管理端会话存 Redis（TTL 6 小时、访问刷新），kaptcha 与登录落到不同实例也能命中同一会话；用户端 JWT 天然无状态。
- 扩容后按 `deploy/cloud/nginx/leomall.conf` 内注释加 upstream，无需 sticky session。
- **多实例的其他前置**：本地存储（`storage.active=local`）需改对象存储或共享卷；未支付订单内存延迟任务有每 10 分钟 DB 扫描兜底，多实例重复触发幂等无害。
- Agent 侧 `REDIS_URL` 启用语义/embedding 缓存与幂等键；`BREAKER_SHARED=1` 熔断状态跨实例共享；队列削峰由 `WORKER_CONCURRENCY` 限住同时在跑的 Agent 轮数。

## 工程质量与 CI（P3）

- **测试已启用**：`server/pom.xml` 不再全局跳测。`cd server && mvn verify` 会编译并运行全部纯单元测试
  （StateStore 语义、BCrypt、富文本消毒、IP 信任边界、金额比较等，31 项）。
  依赖 MySQL/Redis/第三方密钥的 22 个遗留集成测试由 surefire 集中排除，
  需要时在对应环境用 `mvn test -Dtest=XxxTest` 单独运行。
- **CI**：`.github/workflows/main.yml` 按真实目录重建——backend（JDK 8，`server/` 下 `mvn verify`）、
  admin/h5（Node 18 构建，需 `NODE_OPTIONS=--openssl-legacy-provider`）、agent（uv 锁定依赖 + 编译/导入冒烟）。
  Agent pytest 存在 15 个存量夹具失败（演示目录快照不匹配），修复前暂不作为 CI 门禁。
- **devtools** 移入 Maven profile（`-Pdevtools` 启用），默认构建与打包不含热重启依赖；qiniu SDK 锁定精确版本。
- 无 Redis 队列时 Agent 同步接口也有 30 秒窗口幂等：重复提交复用原结果，处理中的并发重复返回 409。

## 接口前缀

后端 REST 前缀统一为 `/leomall/admin/**` 与 `/leomall/wx/**`，前端代理与 Shiro 权限串已同步。

## 说明

本项目为二次开发整合版本。第三方依赖自带的版权声明随其源码保留；
根目录 LICENSE 为项目自身声明。
