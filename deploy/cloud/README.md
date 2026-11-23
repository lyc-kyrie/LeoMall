# LeoMall 云服务器部署方案（Docker Compose 单机版）

适用配置：**4核8G / 60G SSD / Ubuntu 22.04**（2C4G 可跑，见文末降级项）。
架构：Nginx(80) → 两个 Vue 前端静态文件 + 反代 Spring Boot(8080) → MySQL 8 / Python Agent(8000)。
LLM 走外部 API，无需 GPU。

> 说明：`deploy/docker/` 与 `deploy/deploy/` 是旧目录结构（根级 leomall-admin/leomall-vue、MySQL 5.7）
> 的样例，与当前仓库（admin/、h5/、server/、sql/）不匹配，请勿直接使用。本目录为按当前结构重写的方案。
> 另外 `server/leomall-all/pom.xml` 拷贝前端的路径指向不存在的 `../leomall-admin/dist`，
> jar 内不含前端页面 —— 本方案由 Nginx 直接托管前端产物，不依赖 jar 内嵌。

## 0. 架构与端口

```
公网 :80 (Nginx)
 ├─ /            → www/h5      H5 用户端（user123/user123）
 ├─ /admin/      → www/admin   管理后台（admin123/admin123）
 └─ /leomall/**  → backend:8080（含商品图片 /leomall/wx/storage/fetch/）
                     └─ 智能导购 BFF → agent:8000（仅容器内网，X-Internal-Token 鉴权）
backend → mysql:3306（仅容器内网）
```

安全组只放行 **22（建议限源 IP）和 80**；3306/8080/8000/6379 一律不对公网开放。

## 1. 服务器初始化（一次性）

```bash
# Ubuntu 22.04，安装 Docker 与 compose 插件
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER   # 重新登录生效
docker compose version          # 确认 v2 可用

sudo mkdir -p /opt/leomall && sudo chown $USER /opt/leomall
```

2C4G 机器建议加 2G swap：
```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

## 2. 开发机打包（Windows / pwsh 7）

依赖：Node 18+、JDK 8 + Maven；`-ExportCatalog` 时另需 uv、mysql 客户端和本机 MySQL（leomall 库已导入）。

```powershell
cd C:\Users\LIU\Desktop\LYCHome\LeoMall
pwsh -File deploy\cloud\util\package.ps1 -ExportCatalog
```

脚本做四件事：
1. admin/h5 以 **cloud 模式**构建（`.env.cloud`，API 用同源相对路径 `/leomall/admin`、`/leomall/wx`，
   由 Nginx 反代，不写死 IP、无 CORS 问题）；
2. `mvn clean package -DskipTests` 出后端 exec jar；
3. 从 MySQL 导出商品快照（或复用 `agent/data/leomall_catalog.json`）；
4. 组装 `deploy\cloud\package\`：compose、Nginx 配置、jar、前端 dist、Agent 源码最小子集、
   合并后的建库 SQL、商品快照。

上传：
```powershell
scp -r deploy\cloud\package ubuntu@<服务器IP>:/opt/leomall
# 注意：让 package 的内容直接落在 /opt/leomall 下（docker-compose.yml 在其根部）
```

## 3. 服务器配置与启动

```bash
cd /opt/leomall
cp .env.example .env
vim .env
```

`.env` 必改四项：

| 变量 | 说明 |
|---|---|
| `PUBLIC_BASE_URL` | `http://<服务器IP或域名>`，商品图片 URL 前缀，**末尾不带 /** |
| `MYSQL_ROOT_PASSWORD` | root 强随机口令（仅容器内用） |
| `INTERNAL_API_TOKEN` | Java↔Agent 服务间令牌，长随机串 |
| `MYSQL_APP_PASSWORD` | 应用账号口令，**必须与 db/init-sql/leomall.sql 中 `create user 'leomall'@'%' identified by '...'` 一致**；生产把 SQL 里的 `leomall123456` 一并改掉（首次启动前改，数据卷初始化后改 SQL 不再生效，需 `ALTER USER`） |

启动：
```bash
docker compose up -d --build
docker compose ps        # 等 mysql/backend/agent 变 healthy
docker compose logs -f backend   # 看到 Started ... 即就绪
```

首次启动 MySQL 自动执行 `db/init-sql/leomall.sql`（建库+建表+初始数据，仅数据卷为空时执行一次）。

## 4. 验证清单

```bash
curl -s http://127.0.0.1/leomall/wx/index/index | head -c 200   # H5 首页数据接口
curl -s http://127.0.0.1/leomall/admin/index/index | head -c 200 # 后台接口（未登录返回权限提示属正常）
docker exec leomall-agent python -c "import urllib.request;print(urllib.request.urlopen('http://127.0.0.1:8000/health').read().decode())"
# health 里 catalog_count 应 >0；local_fallback=true 表示确定性推荐模式
```

浏览器：
- `http://<服务器IP>/` → H5，登录 user123/user123，首页悬浮导购框发一句"300元内的四件套"应返回商品卡；
- `http://<服务器IP>/admin/` → 管理后台，admin123/admin123；
- 后台上传一张商品图，确认图片 URL 是 `PUBLIC_BASE_URL` 前缀且能打开。

**上线后立即修改默认账号密码**（admin123/user123 是公开的演示口令）。

## 5. 启用真实大模型导购（可选）

默认 `LOCAL_FALLBACK=1`（零密钥确定性推荐）。接入 LLM：

```bash
vim /opt/leomall/.env
# LOCAL_FALLBACK=0
# LLM_API_KEY=sk-xxx
# LLM_MODEL=<控制台确认有效的模型ID>
docker compose up -d agent    # 只重建 agent
```

密钥只存在于服务器 `.env`（chmod 600），不进镜像、不进 Git。
再开 Redis 语义缓存（可选，省 LLM 调用）：`.env` 设 `REDIS_URL=redis://redis:6379/0`，然后
`docker compose --profile cache up -d --build`。

## 6. 日常运维

| 操作 | 命令 |
|---|---|
| 商品改价/上下架后刷新推荐 | 开发机重跑 `-ExportCatalog`，scp 覆盖 `/opt/leomall/agent-data/leomall_catalog.json`，`docker compose restart agent` |
| 更新后端 | 开发机 `package.ps1 -SkipFrontend`，覆盖 jar 后 `docker compose up -d --build backend` |
| 更新前端 | `package.ps1 -SkipBackend`，覆盖 `www/` 后 `docker compose restart nginx` |
| 看日志 | `docker compose logs -f backend agent nginx` |
| 数据库备份 | `docker exec leomall-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" leomall' > backup_$(date +%F).sql` |
| 全停 | `docker compose down`（数据在 db/data、leomall/storage、agent-data 卷，不丢） |

## 7. 2C4G 降级项

- `.env`：`JAVA_OPTS=-Xms256m -Xmx768m`
- `db/conf.d/my.cnf`：`innodb_buffer_pool_size = 256M`
- compose 中 `mem_limit`：mysql 1g / backend 1g / agent 768m
- 不开 Redis profile；构建全部在开发机完成（本方案默认如此）

## 8. 已知边界

- 微信支付/短信/物流为演示配置（`application-core.yml`），未接真实商户号前不要对外承诺支付能力；
  接入时 `notify-url` 需改为公网 HTTPS 地址。
- HTTPS：有域名后建议在 Nginx 前加 certbot（443 + 证书），并把 `PUBLIC_BASE_URL` 改为 https。
- Agent 队列削峰（worker 多进程）默认关闭，单机直跑足够；需要时参考 `agent/docker/docker-compose.yaml`。
- compose 配置已通过本机 `docker compose config` 语法校验，打包脚本已通过 pwsh 语法解析；
  但未在真实云主机上跑过端到端，首次部署按第 4 节清单逐项核对。

## 文件清单

```
deploy/cloud/
├── docker-compose.yml      编排：mysql8 + backend + agent + nginx（+可选 redis）
├── .env.example            环境变量模板（复制为 .env 填写）
├── nginx/leomall.conf      入口路由与反代
├── leomall/Dockerfile      后端镜像（temurin 8-jre，jar 由打包脚本放入）
├── db/conf.d/my.cnf        MySQL 内存调优
├── util/package.ps1        开发机一键打包组装
└── package/                打包产物（git 忽略），整目录上传服务器
```