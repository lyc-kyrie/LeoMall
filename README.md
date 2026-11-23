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

- 后端：Spring Boot 2.1.5 / Java 8 / Maven / MyBatis / Shiro / Druid / MySQL 8
- 前端：Vue2 + vue-cli 3（webpack4），管理后台 Element UI 2.x，H5 Vant2
- Agent：Python 3.11、FastAPI、AgentScope 2.x、Qdrant（可选）、SQLite

## 快速启动

### 1. 数据库

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/leomall_schema.sql
mysql -uroot -p --default-character-set=utf8mb4 leomall < sql/leomall_table.sql
mysql -uroot -p --default-character-set=utf8mb4 leomall < sql/leomall_data.sql
```

建库脚本创建独立数据库 `leomall` 与同名账户，不影响其他数据库。账户口令见 `sql/leomall_schema.sql`，并已与 `server/leomall-db/src/main/resources/application-db.yml` 的 `spring.datasource.druid.password` 对齐；生产部署请修改这两处。

### 2. 后端

```bash
cd server
mvn clean package -DskipTests
java -jar leomall-all/target/leomall-all-0.1.0-exec.jar --server.port=8080
```

需 JDK 8；若环境变量存在 `SERVER__PORT`，启动前先 `unset SERVER__PORT`。

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

`agent/.env` 需与 `server/leomall-wx-api/src/main/resources/application-wx.yml` 中的
`leomall.agent.internal-token` 保持一致。未配置 `LLM_API_KEY` 时自动进入本地确定性推荐模式。

**服务凭据不要写进任何文件**：`LLM_API_KEY` 等通过进程环境变量注入（环境变量优先级高于 `.env`），
生产用平台密钥服务挂载；完整做法见 `agent/README.md` 的「密钥注入」章节。

管理后台默认账号 `admin123 / admin123`；H5 用户账号 `user123 / user123`。

## 接口前缀

后端 REST 前缀统一为 `/leomall/admin/**` 与 `/leomall/wx/**`，前端代理与 Shiro 权限串已同步。

## 说明

本项目为二次开发整合版本。第三方依赖自带的版权声明随其源码保留；
根目录 LICENSE 为项目自身声明。
