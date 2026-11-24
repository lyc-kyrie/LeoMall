# LeoMall 项目审计报告与升级方案

> 审计日期：2026-11-23
> 审计方式：全量静态代码审计（后端 Java / Agent Python / 前端 Vue / SQL / 部署配置 / CI），未执行渗透测试。
> 约定：本报告只给出问题定位、原理分析与修改方案，不直接改动任何代码。文中行号以当前工作区代码为准。

---

## 一、项目详细分析

### 1.1 总体架构

```
H5 用户端 (h5/, Vue2+Vant, :6255) ──/leomall/wx/**──┐
                                                    ├─→ Spring Boot BFF (server/, :8080) ─→ MySQL 8 (leomall)
管理后台 (admin/, Vue2+ElementUI, :9527) ─/leomall/admin/**─┘        │
                                                          X-Internal-Token
                                                                    ↓
                                              智能导购 Agent (agent/, FastAPI+AgentScope, :8000)
                                                    ├─ Qdrant（向量召回，可选）
                                                    ├─ Redis（缓存/队列，可选）
                                                    └─ SQLite（会话/偏好/订单演示域）
```

- 后端为单体多模块 Maven 工程，`leomall-all` 打成一个 fat-jar 同时承载 admin 与 wx 两套 API。
- Java 后端作为 BFF：`@LoginUser` 从 JWT 解析用户身份注入，Agent 的 `buyer_id` 由服务端注入，前端不可伪造；Agent 返回的 `goods_id` 会逐条回查主库校验在售/未删除后才组装商品卡（`WxAgentController.chat`）。
- 生产部署（`deploy/cloud/docker-compose.yml`）只暴露 nginx 80，MySQL/后端/Agent 走容器内网，这部分设计是正确的。

### 1.2 模块清单

| 模块 | 职责 | 关键文件 |
|---|---|---|
| `server/leomall-core` | 存储/通知/物流/系统配置/工具/校验器/CORS/全局异常 | `StorageService`、`LocalStorage`、`CorsConfig`、`JwtHelper` 所在的 wx 模块依赖它 |
| `server/leomall-db` | MyBatis Generator 生成的 domain/dao + 手写 service | `CouponVerifyService`、`LeomallCartService`、`DbUtil` |
| `server/leomall-wx-api` | 用户端 API：认证/购物车/订单/优惠券/导购 | `WxAuthController`、`WxOrderService`、`AgentGateway` |
| `server/leomall-admin-api` | 管理端 API：Shiro RBAC + 全部后台管理 | `ShiroConfig`、`AdminAuthController`、`AdminOrderService` |
| `server/leomall-all(-war)` | 聚合启动模块 | `application.yml`（profiles: db,core,admin,wx） |
| `agent/` | FastAPI + AgentScope 多 Agent（编排/检索/交易），含护栏、熔断、语义缓存、队列 | `presentation/server.py`、`infrastructure/settings.py` |
| `admin/`、`h5/` | Vue2 + vue-cli3（webpack4）双前端 | `h5/src/components/AgentChat`、`admin/src/utils/auth.js` |
| `sql/`、`deploy/`、`.github/` | 建库脚本、单机/容器部署、CI | `leomall_schema.sql`、`cloud/docker-compose.yml`、`workflows/main.yml` |

### 1.3 核心链路现状

**用户认证（wx）**：`/wx/auth/login` → BCrypt 校验 → `JwtHelper.createToken(userId)`（HS256，2 小时）→ 前端存 localStorage，请求带 `X-Leomall-Token` → `LoginUserHandlerMethodArgumentResolver` 解析注入。无 Shiro 过滤链覆盖 wx 接口，鉴权完全依赖各方法自行判断 `userId == null`。

**管理认证（admin）**：Shiro `authc` 过滤链 + `@RequiresPermissions` 注解；登录成功后把 **Shiro Session ID 当 token** 放 `X-Leomall-Admin-Token` 头；会话存内存，6 小时超时；Cookie 回退未关闭。

**下单**：`cart/checkout`（试算）→ `order/submit`（`@Transactional`：建单→建订单商品→清购物车→条件扣库存→置优惠券已用→团购→0 元单直接置已支付，否则挂内存延迟取消任务）→ `prepay/h5pay`（微信统一下单）→ `pay-notify`（验签+金额比对+乐观锁更新）。

**智能导购**：H5 → `wx/agent/chat`（登录态、500 字截断、会话按 userId 隔离）→ `AgentGateway`（HttpURLConnection 同步 POST，read-timeout 60s）→ Agent `/commerce/intents`（内部 token 校验；无 LLM key 时本地确定性推荐）→ 返回推荐 → BFF 回查主库 → 会话/消息/推荐落 MySQL → 管理端可查会话与推荐效果。

### 1.4 设计亮点（值得保留）

1. BFF 身份注入 + 推荐商品主库复核，防 Agent 幻觉/越权，思路正确。
2. 库存扣减是原子条件更新（`GoodsProductMapper.xml`：`set number=number-#{num} where id=? and number>=#{num}`），数据库层面不会超卖。
3. 订单状态更新普遍用乐观锁（`updateWithOptimisticLocker`），支付回调验签、幂等（`hasPayed`）、金额比对都有。
4. `sort/order` 参数有 `@Sort/@Order` 白名单校验器，ORDER BY 注入面已收敛。
5. Agent 侧工程质量明显高于商城本体：分层清晰、有护栏/熔断/幂等/评测集/测试；密钥默认走环境变量，`.env` 已 gitignore。
6. 生产 compose 端口最小暴露、swagger.production 意图、健康检查、内存限额都做了。
7. gitignore 对 `.env`、jar、tar.gz、node_modules、target 的覆盖比较完整（生产 `.env` 未被 git 跟踪，已核实）。

---

## 二、漏洞与缺陷清单

严重程度定义：P0=可直接造成资损/身份接管/权限绕过；P1=可被利用造成明显业务损害；P2=需要一定条件或影响面受限；P3=工程质量与可维护性。

### P0 严重

#### P0-1 JWT 签名密钥硬编码且等于请求头名 → 伪造任意用户身份
- 位置：`server/leomall-wx-api/.../util/JwtHelper.java:19`（`SECRET = "X-Leomall-Token"`），`UserTokenManager`、`LoginUserHandlerMethodArgumentResolver.java:13`。
- 问题：HS256 密钥写死在源码里，且与公开请求头名完全相同，任何读过源码（或猜到）的人都能离线签发任意 `userId` 的合法 token。java-jwt 版本 3.4.1 也已过时。
- 影响：接管任意用户——查看姓名/手机号/地址、下单、退款、读取导购会话。这是全系统最高危问题。
- 方案：
  1. 密钥改为环境变量/密钥服务注入（≥32 字节随机），配置缺失时拒绝启动而不是回退默认值；
  2. 升级 java-jwt 到 4.x，校验时固定算法、校验 issuer/audience/过期；
  3. 引入 `jti` + Redis 黑名单/版本号机制，支持换钥后旧 token 全量失效；
  4. 短期 access token（15~30 分钟）+ refresh token，降低泄露窗口。

#### P0-2 优惠券校验缺归属与状态检查 → 用他人券、单券无限复用、0 元购
- 位置：`server/leomall-db/.../service/CouponVerifyService.java:38-47`；`WxOrderService.submit`（`WxOrderService.java:347-353、440-446、476`）。
- 问题：`checkCoupon` 按 `userCouponId` 查券后**只校验 couponId 匹配**，不校验 `couponUser.getUserId()==userId`，也**完全不校验 `couponUser.getStatus()`**（已用/已过期状态被忽略，只查券模板状态和时间窗）。`submit` 用券后是无条件 `updateByPrimaryKeySelective` 置已用，没有「仅当可用才置已用」的条件更新。
- 影响：
  1. 传别人的 `userCouponId`+对应 `couponId` 即可消耗他人优惠券（水平越权）；
  2. 同一张券在有效期内可反复用于新订单（每单都减免）；
  3. 叠加 `submit` 的「实付 0 元自动置已支付」逻辑（476 行），大额券反复使用可实现 0 元下单，直接资损。
- 方案：
  1. `checkCoupon` 增加归属校验与 `status==USABLE` 校验；
  2. 核销改为条件更新：`update leomall_coupon_user set status=USED, order_id=? where id=? and user_id=? and status=USABLE`，影响行数为 0 则抛异常回滚整单；
  3. `submit` 入口对 `userCouponId` 先做归属断言；
  4. 0 元自动支付加风控开关与审计日志，默认关闭或限制券种。

#### P0-3 管理后台登录验证码被注释 + 无防爆破 + 默认弱口令 + 公网暴露
- 位置：`AdminAuthController.java:91-104`（验证码校验整段注释）；`README.md:70`（admin123/admin123）；`deploy/cloud/nginx/leomall.conf:24`（`/admin/` 公网可达）。
- 问题：登录接口无任何验证码、失败计数、锁定或限流；初始管理员弱口令写在 README 与初始化 SQL；后台与用户端同域同端口暴露。
- 影响：公网自动化爆破即可拿到后台（商品改价、订单退款、管理员创建、存储管理等全部能力）。
- 方案：恢复验证码校验；按「用户名+IP」做失败计数与指数退避锁定（Redis）；初始账号首登强制改密；后台增加 IP 白名单/独立域名/VPN 入口；登录成功审计日志已有（LogHelper），补充失败告警。

#### P0-4 用户端文件上传无鉴权 + 存储类型可控 + 内联回显 → 存储型 XSS / 恶意文件托管
- 位置：`WxStorageController.java:53-58`（`upload` 无 `@LoginUser`、无任何鉴权）；`StorageService.java:46-76`（随机 key+原始后缀，无后缀/ MIME 白名单）；`WxStorageController.fetch:66-83`（用 DB 中客户端上传时声明的 `type` 作为 `Content-Type` 内联返回）。
- 问题：任何人可匿名上传任意类型文件（单文件 20MB，无频率限制）；`fetch` 按上传者控制的 Content-Type 内联渲染。上传 `.html`（Content-Type: text/html）或 `.svg` 后，访问 `/leomall/wx/storage/fetch/{key}` 即在商城同源执行脚本。
- 影响：
  1. 商城域名下的存储型 XSS：诱导用户点击链接即可窃取 localStorage 中的用户 token（与 P0-1 叠加可批量接管）；
  2. 可信域名被用于钓鱼/恶意软件托管；
  3. 匿名刷上传耗尽磁盘（LocalStorage 无配额）。
- 方案：
  1. `upload` 加登录态 + 限流 + 单用户配额；
  2. 后缀与 MIME 双白名单（jpg/png/gif/webp），图片服务端重编码（去 EXIF/脚本），key 后缀由服务端按真实类型生成；
  3. `fetch` 一律 `Content-Disposition: attachment` 或独立无 Cookie 的资源域名，响应加 `X-Content-Type-Options: nosniff`；非图片类型拒绝内联；
  4. `generateKey` 对无后缀文件名做防御（当前 `lastIndexOf('.')==-1` 会抛异常返回 500）；key 规范化并禁止路径分隔符（见 P2-8）。

#### P0-5 Shiro 1.6.0 存在认证绕过 CVE，Spring Boot 2.1.5 全家桶 EOL
- 位置：`server/pom.xml:12、76`；`ShiroConfig.java:39`（`/leomall/admin/** = authc`）。
- 问题：
  1. Shiro <1.7.1 受 CVE-2020-17523 影响（与 Spring 集成时，构造含空格/%20 等特殊路径可绕过 authc 过滤链）；Shiro <1.12.0 受 CVE-2023-34478 影响（与 Spring 等会做路径规范化的框架配合时，`/..;/` 类路径可绕过链匹配）。两者都直接命中本项目「Shiro 过滤链 + Spring MVC」的组合。
  2. Spring Boot 2.1.5（2019 年）/Spring Framework 5.1.x 早已停止维护，累计大量 CVE（含 Spring4Shell 相关家族；项目还存在 `leomall-all-war` 模块，WAR+外置 Tomcat 部署时风险面更大）。
- 影响：未认证访问管理端接口（商品/订单/管理员/存储），等价于后台沦陷。
- 方案：
  1. 紧急：Shiro 升到 1.13.x（1.x 最新），并在 nginx/网关层对 `/leomall/admin/` 增加路径规范化与可疑路径（`%20`、`..;`、双重编码）拦截；
  2. 中期：Boot 升 2.7.18（Java 8 可用的最后版本）作为跳板，再迁 Boot 3.x + Java 17；
  3. 长期：用 Spring Security 6 替换 Shiro（过滤链与 Spring 路径匹配同源，消除这类「两套路径语义」绕过），或至少保证 Shiro 与 Spring 的路径匹配策略一致并加集成测试覆盖绕过用例。

#### P0-6 Agent 多个端点完全无鉴权 + 开发编排公网暴露 8000
- 位置：`agent/app/presentation/server.py:336-384`（`/commerce/intents/async`、`/commerce/tasks/{id}`、`/commerce/orders/{id}`、`/commerce/orders/{id}/cancel` 均无 token 校验）；`connection.py:25-47`（WS 报任意 session_id 即可订阅事件流）；`order_usecases.py:68-97`（Query/Cancel 不校验 buyer 归属）；`agent/Dockerfile:38`（`--host 0.0.0.0`）；`agent/docker/docker-compose.yaml:16-17、86-87、99-100`（8000/6379/6333 全部映射到宿主机，且未注入 `INTERNAL_API_TOKEN`）。
- 问题：只有同步 `/commerce/intents` 做了内部 token 校验（且 token 为空时直接放行）；其余端点「裸奔」。开发 compose 把 Agent、无密码 Redis、无鉴权 Qdrant 一起暴露到所有网卡。
- 影响：凡能访问 8000 的人（同机其他进程、SSRF、误用 dev compose 上云）可：伪造任意 buyer_id 提交意图（消耗 LLM 配额）、读取/取消任意订单、窃听任意会话事件流；Redis/Qdrant 可被未授权读写。
- 方案：
  1. 把内部 token 校验提为 FastAPI 全局依赖（Depends），覆盖全部 HTTP 与 WS（WS 在订阅消息里带 token 并校验 session 归属）；
  2. Query/Cancel UseCase 增加 `buyer_id` 归属断言；
  3. dev compose 端口映射改 `127.0.0.1:8000:8000` 等，Redis 加 requirepass，Qdrant 加 API key 或取消映射；
  4. 生产已有「仅内网」约定，补一条启动自检：`HOST!=127.0.0.1` 且 token 为空时拒绝启动。

### P1 高危

#### P1-1 CORS 通配符 + 允许凭据 → 任意网站跨站调用管理接口
- 位置：`server/leomall-core/.../config/CorsConfig.java:16-20`。
- 问题：`addAllowedOrigin("*")` 且 `setAllowCredentials(true)`。Spring 5.1 在该组合下会**反射请求 Origin** 并返回 `Access-Control-Allow-Credentials: true`。管理端会话 Cookie 回退未禁用（`AdminWebSessionManager.java:21` 注释掉了 `setSessionIdCookieEnabled(false)`），admin 前端 token 也存于无 SameSite 的 Cookie（`admin/src/utils/auth.js`）。
- 影响：管理员登录状态下访问任意恶意页面，该页面即可带凭据跨域读取/调用全部管理 API（比传统 CSRF 更强，能读响应）。
- 方案：Origin 改显式白名单（配置化）；管理端禁用 Cookie 会话、纯 Header token；Cookie 加 `SameSite=Lax/Strict`；预检缓存 30 天过长，降到分钟级。

#### P1-2 订单提交 cartId 路径水平越权 + 空指针
- 位置：`WxOrderService.java:326`（`cartService.findById(cartId)` 无 userId 重载）对比 `WxCartController.checkout:444`（用了 `findById(userId, cartId)`）。
- 问题：`submit` 的立即购买路径按主键取任意购物车行，不校验归属；随后 `cartService.deleteById(cartId)`（422 行）会删除他人购物车；`cart==null` 时直接 NPE→500。
- 影响：越权使用/删除他人购物车项；枚举 cartId 探测他人商品与价格；脏参数打崩请求。
- 方案：统一改用 `findById(userId, cartId)`；null 判断返回 badArgument；删除同样带 userId 条件。

#### P1-3 优惠券领取/兑换 check-then-act 竞态 → 突破限领
- 位置：`WxCouponController.receive:193-242`、`exchange:271-321`。
- 问题：「查已领数量→比较→插入」全程无事务、无锁、无唯一约束。并发请求同时通过检查后各自插入。
- 影响：单用户并发绕过每人限领与总量限制，批量薅券（叠加 P0-2 直接资损）。
- 方案：
  1. 数据库唯一索引兜底（如 `coupon_user(coupon_id,user_id)` 在限领=1 时；限领>1 用计数表+条件更新）；
  2. 总量用 `update coupon set received=received+1 where id=? and (total=0 or received<total)` 原子扣减；
  3. 或 Redis 分布式锁（Redisson）按 userId+couponId 串行化；整个方法加 `@Transactional`。

#### P1-4 短信验证码体系缺陷：轰炸、复用、爆破、内存泄漏
- 位置：`WxAuthController.regCaptcha:186-207`（无鉴权）、`reset:381-415`、`CaptchaCodeManager.java`。
- 问题：
  1. regCaptcha 匿名可调，仅「同号码 1 分钟」限制，无 IP/设备/全局频率限制 → 对任意号码轰炸、跨号码刷短信费；
  2. 验证码校验通过后**不销毁**，有效期内可重复用于 register/reset；
  3. 无错误次数限制，6 位数字码在窗口内可被在线爆破；
  4. ConcurrentHashMap 过期条目永不清理（缓慢内存泄漏），且多实例不共享。
- 方案：验证码存 Redis（TTL 即过期）；校验成功立即删除；每码错误上限 5 次即作废；发送侧加 IP+号码+全局三维限流与图形验证码前置；reset 成功后使该手机号全部会话/token 失效。

#### P1-5 微信登录把 openId 当明文密码入库
- 位置：`WxAuthController.loginByWeixin:143`（`user.setPassword(openId)`）；`LeomallUserService.add:38-42`（不做哈希）。
- 问题：openId 明文存进 password 列；register 的绑定检查（287 行）也依赖明文比对。openId 并非秘密（前端/日志多处可见）。
- 影响：知道 openId 即可走 `/wx/auth/login` 账号密码登录接管该用户；DB 泄露即明文密码泄露（合规问题）。
- 方案：微信注册用户 password 置随机不可登录值（或 NULL+登录方式标记），绑定关系单独用 `weixin_openid` 列判断；存量数据清洗。

#### P1-6 下单价格用购物车快照，submit 不复核现价与在售状态
- 位置：`WxOrderService.submit:333-341`（用 `cart.getPrice()`）；对比 `WxCartController.add:155`（加购时写快照）。
- 问题：加购后管理员改价/下架，submit 仍按旧价成交，也不重新校验 `goods.isOnSale`（checkout 有校验但 submit 没有，前端可绕过 checkout 直接 submit）。
- 影响：涨价后按旧价买入（资损）；下架商品仍可成交。
- 方案：submit 内逐条回查 product 现价与 goods 在售状态，价格不一致则拒绝并提示刷新；快照价仅用于展示。

#### P1-7 凭据治理问题（需立即轮换）
- 位置与事实：
  1. `application-core.yml:4-5、104-105`：wx app-secret、腾讯 COS secretId/secretKey **已被 git 跟踪**（源自 litemall 演示值，但必须按真实泄露处理）；
  2. `application-db.yml:13` 与 `sql/leomall_schema.sql:6`：DB 口令 `leomall123456` 入库，账户 `'leomall'@'%'` 全权限；
  3. `deploy/cloud/package/.env`：**真实生产凭据**（公网 IP、MySQL root/应用口令、内部 token）存在于工作区与 `leomall-package.tar.gz` 中——虽未进 git（已核实 gitignore 生效），但随包分发/截图/拷贝即泄露；
  4. `application-wx.yml:9`：内部 token 回退默认值 `leomall-agent-dev-2026` 随 jar 分发，生产忘配环境变量即等于公开。
- 方案：立即轮换上述全部凭据；git 历史中的密钥用 gitleaks 扫描评估；配置全部改「环境变量必填、无默认值」；部署包与源码分离，`.env` 只留 `.env.example`；DB 账户限制来源主机、按最小权限拆分（应用账号去掉 DDL）。

#### P1-8 导购聊天无限流 + 60 秒同步阻塞 → 成本滥用与线程池耗尽
- 位置：`WxAgentController.chat`（无频率/配额控制）；`AgentGateway.java:49-50`（read-timeout 60s，HttpURLConnection 同步）；nginx `proxy_read_timeout 90s`。
- 问题：每个登录用户可无限发起 chat，每轮可能触发 LLM+embedding+rerank 调用；Tomcat 工作线程被同步阻塞最长 60s。
- 影响：脚本刷量烧光 LLM 配额；数十并发即可占满 Tomcat 线程池，整站（含普通下单）不可用。
- 方案：按 userId 令牌桶限流（如 6 次/分钟、每日配额）；Java 侧改异步（Servlet 3.1 async + 独立线程池/ WebClient）并加熔断（resilience4j）与更短超时；Agent 侧已有并发闸门，BFF 侧也要有；chat 结果落库与响应解耦。

### P2 中危

#### P2-1 金额比较用 BigDecimal.equals / 字符串相等
- 位置：`WxOrderService.java:476`（`equals(new BigDecimal("0.00"))`，scale 敏感）、`773`（回调金额 `totalFee.equals(actualPrice.toString())`）。
- 影响：`0` 与 `0.00` 判不等 → 0 元单分支可能不触发；回调金额格式差异导致合法支付通知被拒（掉单）。
- 方案：一律 `compareTo(...) == 0`；金额入库统一 scale=2。

#### P2-2 内存态组件导致重启丢状态、无法水平扩展
- 位置：`TaskService`/`OrderUnpaidTask`（内存延迟队列）、`CaptchaCodeManager`（内存 Map）、Shiro 内存会话、`HomeCacheManager`。
- 影响：重启后未支付订单永不超时取消（库存被占）；验证码/会话全丢；多实例部署行为不一致。
- 方案：订单超时改「DB 扫描兜底任务 + 延迟消息（Redisson/RocketMQ）」；验证码/会话/缓存迁 Redis；Shiro 会话接 Redis 或干脆无状态化。

#### P2-3 订单提交无幂等
- 位置：`WxOrderService.submit`。
- 影响：双击/网络重试/并发产生重复订单与重复扣库存。
- 方案：客户端幂等键（checkout 返回 token，submit 携带，Redis setnx 去重）；或按 userId+cartId+时间窗去重。

#### P2-4 IpUtil 无条件信任 X-Forwarded-For
- 位置：`core/util/IpUtil.java:20`。
- 影响：审计日志、登录 IP、支付 spbillCreateIp 均可伪造；未来基于 IP 的限流会被绕过。
- 方案：只信任已知代理层数（nginx 设置 X-Real-IP，后端取最后一跳可信代理追加的值）。

#### P2-5 数据库备份实现不可靠
- 位置：`db/util/DbUtil.java:13-14`、`admin/job/DbJob.java`。
- 问题：`Runtime.exec` 字符串拼接（口令出现在进程命令行，可被同机用户窥视；含空格口令直接失败）；不检查退出码、异常吞掉 → 备份可能静默失败/空文件；无压缩加密、无异地。
- 方案：ProcessBuilder 参数数组 + `MYSQL_PWD` 环境变量或 `--defaults-extra-file`；校验退出码与文件大小；mysqldump 加 `--single-transaction`；备份加密后传对象存储；恢复演练。

#### P2-6 Swagger 无条件启用，production 开关是空参数
- 位置：`AdminSwagger2Configuration.java`（无 `@ConditionalOnProperty`）；`application.yml:22` 与 compose `--swagger.production=true` 无任何代码消费。
- 影响：8080 直接可达的环境（开发/误暴露）API 文档全公开；springfox 2.9.2 / swagger-bootstrap-ui 1.9.6 老旧。
- 方案：Docket 加 `@ConditionalOnProperty(name="swagger.production", havingValue="false")` 或按 profile 装配；升级 springdoc-openapi；nginx 层禁止转发文档路径。

#### P2-7 管理端接口回显密码哈希 + RBAC 无层级
- 位置：`AdminAdminController.create:86`（`ok(admin)` 含 bcrypt 哈希，read/update 同类）；角色创建无「不得授予高于自身的权限」约束。
- 影响：哈希外泄助长离线爆破；拥有管理员管理权限的账号可自提为超级管理员（`*`）。
- 方案：响应 DTO 剔除 password 字段；角色/权限授予加层级校验与审计；超级管理员操作二次验证。

#### P2-8 存储管理可任意改 key/type + fetch  traversal 检查薄弱
- 位置：`AdminStorageController.update:70-75`（整对象更新，key/url/type 可改）；`WxStorageController.fetch:72`（仅查 `../`，不防 `..\`、编码变体；Windows 部署尤其注意）。
- 影响：低权限管理账号篡改存储记录指向任意路径/类型，配合 fetch 内联回显扩大 XSS/任意文件读取面（需 key 在 DB 中存在，利用受限）。
- 方案：update 只允许改 name 等展示字段；key 服务端规范化（拒绝分隔符与 `..`），`loadAsResource` 后校验 `normalize().startsWith(root)`；type 由服务端探测。

#### P2-9 富文本 v-html 与前端 token 存储
- 位置：`h5/src/views/items/detail/index.vue:48`、`topic/index.vue:4`；`admin/src/views/goods/list.vue:65` 等 4 处；`admin/src/utils/auth.js`（js-cookie 无 Secure/HttpOnly/SameSite）；h5 token 在 localStorage。
- 影响：管理员录入的富文本未消毒直达用户端（管理员账号被盗/内部恶意 → 全站存储型 XSS）；XSS 一旦发生 token 直接可窃。
- 方案：富文本入库前服务端白名单消毒（jsoup safelist）或渲染前 DOMPurify；token Cookie 加 SameSite+Secure（HttpOnly 需配合 BFF 改造）；上 CSP。

#### P2-10 传输与入口安全缺失
- 位置：`deploy/cloud/nginx/leomall.conf`（仅 80，`PUBLIC_BASE_URL=http://IP`）；无安全响应头、无 limit_req。
- 影响：token/密码明文传输可被中间人窃取；无入口限流。
- 方案：TLS（证书+80 跳转 443+HSTS）；加 `X-Content-Type-Options/X-Frame-Options/CSP/Referrer-Policy`；nginx 对登录/验证码/上传/chat 加 limit_req  zone。

#### P2-11 Agent 数据一致性与存储选型
- 位置：`deploy/cloud/docker-compose.yml:84-85`（商品快照手动导出+重启替换）；`agent/docker/docker-compose.yaml:68-69`（app 与 worker 共享 SQLite 文件）；`server.py /health`（无鉴权回显模型名/目录规模）。
- 影响：改价/上下架后召回与排序依据过期快照（商品卡价格因 BFF 复核是实时的，但召回可能推已下架品）；SQLite 双进程写锁竞争，不适合生产多进程；健康检查信息泄露。
- 方案：快照改定时增量同步（binlog/CDC 或定时导出+版本号热加载）；会话/偏好迁 MySQL/PostgreSQL（settings 已预留 DATABASE_URL）；/health 拆分 liveness（公开）与 readiness（带 token）。

#### P2-12 MySQL 连接与账户配置
- 位置：`application-db.yml:10`（`useSSL=false&allowPublicKeyRetrieval=true&verifyServerCertificate=false`）；`sql/leomall_schema.sql:6-7`（`'leomall'@'%'` + all privileges）。
- 影响：跨主机部署时凭据与数据明文传输；账户来源不限。
- 方案：生产启用 TLS 与证书校验；账户限定容器网段/主机；应用账号最小权限。

### P3 低危与工程质量

| # | 位置 | 问题 | 建议 |
|---|---|---|---|
| P3-1 | `.github/workflows/main.yml:50、68` | CI 引用不存在的 `leomall-admin/`、`leomall-vue/` 目录（已改名 admin/h5），前端 job 必挂 | 修正路径；矩阵换 Node 18/20 |
| P3-2 | `server/pom.xml:19` | `maven.test.skip=true` 全局跳测，CI `mvn verify` 形同虚设 | 删除该属性，修复并启用测试 |
| P3-3 | `server/pom.xml:131` | qiniu 版本区间 `[7.2.0,7.2.99]` → 构建不可复现 | 锁定精确版本 |
| P3-4 | `server/pom.xml:193-197` | devtools 进父 POM 全模块；WAR 模块需确认不打包 | 移入 profile 或确认 repackage 排除 |
| P3-5 | 多处 | `e.printStackTrace()`（WxAuthController、WxOrderService 等） | 统一 slf4j 占位符日志 |
| P3-6 | `WxAgentController.history:240-241` | 全量加载会话消息再截断 100 条 | SQL 层 limit/分页 |
| P3-7 | `miniprogram/` | 保留两套小程序旧代码，与主推 H5 并存 | 归档或移除，减少维护面 |
| P3-8 | `README.md:70` | 默认账号口令明文写在 README | 改「首登强制改密」说明 |
| P3-9 | 测试依赖 | mockito 1.10.19 / powermock 1.6.6 极旧，与 JUnit5 不兼容 | 升 mockito 5 + junit-jupiter |
| P3-10 | Agent 同步路径 | 无 Redis 时 `/commerce/intents` 直跑无幂等保护（幂等只在 `_enqueue`） | 同步路径也加幂等键 |

---

## 三、依赖升级对照表（后端）

| 依赖 | 当前 | 目标（阶段一） | 目标（阶段二） | 说明 |
|---|---|---|---|---|
| Spring Boot | 2.1.5.RELEASE | 2.7.18 | 3.3.x/3.4.x | 2.7 是 Java8 最后支持版；3.x 需 Java17 |
| Java | 8 | 8（过渡） | 17 LTS | Boot3 前置条件 |
| Shiro | 1.6.0 | 1.13.x | 替换为 Spring Security 6 | 修 CVE-2020-17523 / CVE-2023-34478 |
| java-jwt | 3.4.1 | 4.4.x | 4.4.x | 配合密钥外置 |
| mybatis-spring-boot-starter | 1.3.2 | 2.3.x | 3.0.x | 与 Boot 版本对齐 |
| pagehelper-starter | 1.2.5 | 1.4.7 | 2.1.x | 同上 |
| druid-starter | 1.2.1 | 1.2.23+ | 1.2.23+ | 监控面已关，保持 |
| mysql-connector-java | 8.0.28 | com.mysql:mysql-connector-j 8.4 | 9.x | GA 坐标已变更 |
| weixin-java-pay/miniapp | 4.1.0 | 4.6.x | 4.6.x | 支付解析与证书处理修复 |
| springfox + bootstrap-ui | 2.9.2 / 1.9.6 | springdoc-openapi 1.7.x | springdoc 2.x | springfox 与 Boot2.6+ 不兼容 |
| aliyun-sdk-oss | 2.5.0 | 3.17.x | 3.17.x | 老版本 TLS/依赖问题 |
| qiniu-java-sdk | 区间版本 | 锁定 7.2.x 精确版 | 7.15+ | 可复现构建 |
| guava | 32.0.0-jre | 33.x | 33.x | 当前无已知高危，常规升级 |
| 前端 Vue | 2.x（已 EOL） | 2.7.15（过渡） | Vue3 + Element Plus / Vant4 + Vite | webpack4+Node18 需 legacy-provider 的根因消除 |

> 升级顺序建议：先 Shiro/jwt/connector 等点状安全升级（不动 Boot）→ Boot 2.7.18 整体跳版（javax 仍可用）→ Java17 + Boot3（javax→jakarta 大改）→ 前端 Vue3。每步独立可回滚。

---

## 四、分阶段升级路线图

### 阶段 0：紧急止血（1~3 天，不动架构）

目标：消除可直接资损/接管的漏洞。全部为小改动，可逐项独立上线。

1. 轮换全部凭据（P1-7）：DB、wx、COS、内部 token、生产 .env；配置改环境变量必填。
2. JWT 密钥外置 + 随机化（P0-1），旧 token 全量失效（换钥即失效）。
3. `checkCoupon` 补归属+状态校验；核销改条件更新（P0-2）。
4. 恢复后台验证码 + 登录限流锁定 + 首登强制改密（P0-3）。
5. `wx/storage/upload` 加登录态；fetch 加 nosniff + 非图片 attachment；后缀白名单（P0-4）。
6. Shiro 升 1.13.x；nginx 拦截可疑编码路径（P0-5 紧急部分）。
7. Agent 全端点统一内部 token 依赖 + 订单归属校验；dev compose 端口收回环（P0-6）。
8. `submit` 改 `findById(userId, cartId)` + null 防御（P1-2）；金额比较改 compareTo（P2-1）。
9. CORS 白名单化；管理端禁用 Cookie 会话（P1-1）。

验收：用越权/复用/伪造 token 用例回归（见附录清单）；gitleaks 扫描通过；默认口令登录被拒。

### 阶段 1：安全与可靠性加固（1~2 周）

1. 引入 Redis：验证码（TTL+错误计数+销毁）、登录/短信/chat 限流、Shiro 会话或无状态 JWT+黑名单、订单幂等键。
2. 优惠券领取/兑换原子化（唯一索引+条件更新/分布式锁）（P1-3）。
3. 短信风控三维限流 + 图形验证码前置（P1-4）；微信注册密码改随机值+存量清洗（P1-5）。
4. submit 复核现价/在售（P1-6）；0 元单风控开关。
5. 订单超时改 DB 扫描兜底 + 延迟队列（P2-2）；备份改 ProcessBuilder+退出码校验+异地（P2-5）。
6. chat 异步化 + 熔断 + 每用户配额（P1-8）。
7. TLS + 安全头 + nginx 限流 + 后台 IP 白名单（P2-10、P0-3）。
8. Swagger 条件装配（P2-6）；管理端响应剔除密码哈希（P2-7）；富文本消毒（P2-9）。
9. CI 修复：正确路径、启用测试、加 gitleaks + OWASP dependency-check + Trivy 镜像扫描（P3-1/2）。

验收：并发薅券/重复下单/重启丢单三类故障注入测试通过；依赖扫描无高危；备份可恢复演练成功。

### 阶段 2：架构与工程升级（1~2 个月）

1. Boot 2.7.18 → Java17 + Boot3.x；Shiro → Spring Security 6（统一路径匹配语义，根治绕过类问题）。
2. 认证体系重构：短期 access + refresh、jti 黑名单、设备绑定可选；管理端 MFA。
3. 存储迁对象存储（OSS/COS + STS 直传），本地存储仅开发用；图片处理流水线。
4. Agent：会话/偏好迁 MySQL/PostgreSQL；商品快照自动同步（CDC/定时+版本热加载）；订单演示域与真实商城域明确隔离或下线（ENABLE_TRADE_TOOLS 保持关闭并移除对外端点）。
5. 前端 Vue3 + Vite + Element Plus/Vant4，消除 webpack4/legacy-provider；token 存储与 CSP 配套改造。
6. 可观测性：actuator+Prometheus+Grafana、结构化日志、链路追踪（Agent 已有 OTLP，Java 侧接入同一 collector）；告警（登录失败、退款、0 元单、Agent 降级率）。
7. 测试体系：核心业务集成测试（Testcontainers：优惠券竞态、订单越权、库存并发、支付回调幂等）；恢复并扩充 Agent 评测集回归。
8. 数据库治理：账户最小权限+来源限制、TLS、审计表、软删除一致性检查。

### 附录：回归验收用例清单（阶段 0 必测）

| 用例 | 预期 |
|---|---|
| 用旧 SECRET 离线签发 userId=1 的 token 调 `/wx/user/info` | 401 |
| 用户 A 传用户 B 的 userCouponId 下单 | 拒绝 |
| 同一 userCouponId 连续下单两次 | 第二单拒绝 |
| 并发 20 请求 receive 限领 1 张的券 | 仅 1 成功 |
| 匿名 POST `/wx/storage/upload` | 401 |
| 上传 html 后访问 fetch | attachment/nosniff，不执行 |
| 后台连续错密 10 次 | 锁定/验证码强制 |
| `GET /leomall/admin/%20goods/list` 等绕过变体 | 401 |
| 无 token 调 Agent `/commerce/orders/x/cancel` | 401 |
| 用户 A 传他人 cartId submit | 拒绝 |
| 管理员改价后按旧快照 submit | 拒绝并提示刷新 |
| 0.00 / 0 两种 scale 的 0 元单 | 行为一致 |

---

*报告完。建议按阶段 0 清单逐项建立 issue 跟踪，每项附本报告对应编号。*

---

## 五、P0 修复记录（2026-11-23，已实施）

> 以下为已落地的代码修改；P1/P2 仍按原路线图推进。

| 编号 | 修改内容 | 涉及文件 |
|---|---|---|
| P0-1 | JWT 密钥改为启动注入（`leomall.jwt-secret` / 环境变量 `LEOMALL_JWT_SECRET`，≥32 字符，缺失拒绝启动）；校验增加 audience 与空 claim 防御；java-jwt 3.4.1→4.4.0；换钥即令存量 token 全失效 | `JwtHelper.java`、新增 `JwtSecretInitializer.java`、`application-wx.yml`、`server/pom.xml`、`deploy/cloud/docker-compose.yml`、`.env.example` |
| P0-2 | `checkCoupon` 增加归属/删除/可用状态校验；核销改原子条件更新 `markUsed`（影响行数 0 回滚整单）；0 元判断改 `compareTo` | `CouponVerifyService.java`、`LeomallCouponUserService.java`、`WxOrderService.java` |
| P0-3 | 恢复验证码（一次性、空值防御）；新增 `AdminLoginGuard` 失败锁定（5 次/15 分钟）；初始弱口令登录返回 `mustChangePassword`，前端路由守卫强制跳转改密页 | `AdminAuthController.java`、新增 `AdminLoginGuard.java`、`admin/src/views/login/index.vue`、`store/modules/user.js`、`permission.js`、`views/profile/password.vue` |
| P0-4 | 上传加登录态；后缀白名单（jpg/jpeg/png/gif/webp/bmp）+ 分隔符防御 + 无后缀防御；fetch/download 的 Content-Type 改服务端按后缀判定，非图片附件下载，全部加 nosniff；key 拒绝 `..`/`/`/`\`；LocalStorage 读取/删除加根目录边界校验 | `WxStorageController.java`、`StorageService.java`、`LocalStorage.java`、nginx `leomall.conf` |
| P0-5 | Shiro 1.6.0→1.13.0（修 CVE-2020-17523 / CVE-2023-34478）；nginx 对解码后路径中的 `..`、`;`、`%`、空白直接 400 | `server/pom.xml`、`deploy/cloud/nginx/leomall.conf` |
| P0-6 | Agent 全 HTTP 端点统一 `require_internal_token` 依赖；订单查询/取消强制 `buyer_id` 并校验归属；WS 订阅校验内部 token；启动自检（无 token 且非回环绑定拒绝启动）；Dockerfile 绑定地址改由 HOST 决定（默认回环）；dev compose 端口全部收回环、Redis/Qdrant 取消宿主机映射、token 必填；演示前端携带 token；Java 侧内部 token 移除默认值 | `agent/app/presentation/server.py`、`connection.py`、`order_usecases.py`、`agent/Dockerfile`、`agent/docker/docker-compose.yaml`、`agent/frontend/src/App.tsx`、`application-wx.yml` |

**验证**：后端 `mvn compile` 7 个模块全部 BUILD SUCCESS（含 Shiro 1.13.0 / java-jwt 4.4.0，日志 `server/build-check.log`）；Agent 改动文件 `py_compile` 通过；Agent `pytest` 276 通过 / 15 失败——失败均为存量问题（测试夹具引用演示目录 P1001/P1006，实际加载 leomall 商品快照），失败点位于本次未改动的 PlaceOrder/检索/工具代码路径，与 P0 修复无关。

**残留风险（需阶段 1 跟进）**：
1. 强制改密目前为前端守卫拦截，服务端未对未改密会话做接口级封禁；
2. 登录锁定/验证码仍为单实例内存态，多实例部署前必须迁 Redis；
3. 凭据轮换（P1-7）需运维执行：git 历史中的 wx/COS 演示密钥、DB 口令、本地部署包 .env；
4. Boot 2.1.5 本体 CVE 需阶段 2 升级解决；CORS 白名单（P1-1）、submit 越权（P1-2）等 P1 项尚未修复。

---

## 六、P2 修复记录（2026-11-24，已实施）

| 编号 | 修改内容 | 涉及文件 |
|---|---|---|
| P2-1 | 支付回调金额比较改 `BigDecimal.compareTo`（原字符串相等受 scale/格式影响会误拒合法通知） | `WxOrderService.java` |
| P2-2 | `OrderJob` 新增每 10 分钟未支付订单 DB 扫描兜底（乐观锁取消 + 回补库存 + 返还优惠券，与内存延迟任务互为幂等）；`CaptchaCodeManager` 过期条目即取即删 + 机会性批量清理 | `OrderJob.java`、`CaptchaCodeManager.java` |
| P2-3 | `submit` 增加 10 秒窗口防重复提交（Guava Cache，事务回滚自动释放占位）；**单实例内存实现，多实例前迁 Redis** | `WxOrderService.java` |
| P2-4 | `IpUtil` 仅当直连对端为可信代理（回环/内网）时读取 X-Real-IP/XFF，否则用 TCP 对端地址，杜绝客户端伪造审计 IP | `IpUtil.java`（nginx 已设 X-Real-IP） |
| P2-5 | `DbUtil` 改 ProcessBuilder 参数数组 + `MYSQL_PWD` 环境变量（口令不再出现在进程命令行）+ `--single-transaction` + 退出码/空文件校验，失败抛异常不再静默 | `DbUtil.java` |
| P2-6 | 两套 Swagger 配置加 `@ConditionalOnProperty(swagger.production=false)`，生产开关真正生效 | `AdminSwagger2Configuration.java`、`WxSwagger2Configuration.java` |
| P2-7 | 管理员 list/create/read 响应剔除密码哈希；非超管不得授予超管角色、不得编辑超管账号 | `AdminAdminController.java` |
| P2-8 | 存储记录 update 仅允许改 name，key/url/type/size 不可被客户端篡改 | `AdminStorageController.java` |
| P2-9 | 新增 jsoup 1.17.2 + `RichTextUtil` 白名单消毒，商品详情/专题/公告入库前清洗；管理端 Cookie `SameSite=Lax`（HTTPS 加 Secure），js-cookie 升 2.2.1 | 新增 `RichTextUtil.java`、`AdminGoodsService.java`、`AdminTopicController.java`、`AdminNoticeController.java`、`admin/src/utils/auth.js`、`admin/package.json`、父子 pom |
| P2-10 | nginx 安全头（X-Frame-Options/Referrer-Policy）；登录/验证码/重置、上传、导购聊天按路径限流（map+空键，单 location 实现），超限 429；附 TLS 就绪模板 | `deploy/cloud/nginx/leomall.conf` |
| P2-11 | 公开 `/health` 最小化（仅存活+依赖状态），内部详情移入需 token 的 `/health/detail`；商品快照定时热加载（`CATALOG_REFRESH_SECONDS` 默认 300s，快照为空保留原目录） | `agent/app/presentation/server.py`、`agent/app/infrastructure/settings.py` |
| P2-12 | 应用 DB 账号去除 DDL，仅保留 DML + 备份所需权限（SHOW VIEW/TRIGGER/LOCK TABLES/EVENT） | `sql/leomall_schema.sql` |

**验证**：后端 `mvn compile` 7 个模块全部 BUILD SUCCESS（含 jsoup 1.17.2，日志 `server/build-check.log`）；Agent 改动文件 `py_compile` 通过；Agent `pytest` 276 通过 / 15 失败，失败清单与 P0 轮完全一致（存量演示目录夹具问题），无新增回归。

**残留风险（阶段 1/2 跟进）**：
1. Shiro 会话、短信验证码、登录锁定、下单去重仍为单实例内存态——多实例部署前必须迁 Redis（阶段 1）；
2. TLS 需证书后按 nginx 模板启用；CSP 待前端改造后启用；
3. 快照热加载只覆盖 API 进程与关键词召回，Qdrant 向量索引重建仍走管理端手动触发，worker 进程快照需重启刷新；
4. P1 项（CORS 白名单、submit cartId 越权、领券竞态、短信风控、微信明文密码、凭据轮换等）仍未修复，建议下一轮推进。

## 七、P1 修复记录（2026-11-24，已实施）

| 编号 | 修改内容 | 涉及文件 |
|---|---|---|
| P1-1 | CORS 来源白名单化：`leomall.cors.allowed-origins`（env `LEOMALL_CORS_ALLOWED_ORIGINS`，逗号分隔），**默认空=不开放任何跨域**；仅白名单非空才允许凭据；预检缓存 30 天→10 分钟。双前端 devServer 均为同源代理、生产 nginx 同源，不受影响。Shiro 会话 Cookie 加固为 HttpOnly+SameSite=Lax（kaptcha 依赖 Cookie 会话，不能禁用），业务请求仍走 `X-Leomall-Admin-Token` 头 | `CorsConfig.java`、`AdminWebSessionManager.java`、`application-core.yml` |
| P1-2 | `submit` cartId 路径改 `findById(userId, cartId)` 归属校验 + null 防御；下单后删购物车改带归属的逻辑删除 `deleteById(userId, cartId)` | `WxOrderService.java`、`LeomallCartService.java` |
| P1-3 | receive/exchange 加「用户+券」Striped 串行锁，锁内 `TransactionTemplate` 事务复查限领/总量后插入（事务在锁释放前提交），消除 check-then-act 竞态。**单实例内存锁，多实例前迁 Redis 分布式锁或 DB 唯一约束** | 新增 `CouponClaimLock.java`、`WxCouponController.java` |
| P1-4 | 短信风控：同一号码每天 ≤10 条、同一 IP 每小时 ≤20 条（叠加原 1 分钟间隔）；统一 `checkCaptcha` 入口——成功即销毁（一次性使用）、连续失败 5 次作废；register/reset/resetPhone 全部接入 | `CaptchaCodeManager.java`、`WxAuthController.java` |
| P1-5 | 微信自动建号不再把 openId 明文当密码，改随机 32 位 BCrypt 不可登录密码；register 的 openid 绑定判断改「username==openId 且未绑手机号」识别壳账号，不再依赖明文密码比对 | `WxAuthController.java` |
| P1-6 | `submit` 对每件商品复核主库现价与在售状态：下架拒绝（710）、快照价与现价不一致拒绝（722，提示刷新重下） | `WxOrderService.java` |
| P1-7 | wx `app-secret`、腾讯 COS `secretId/secretKey` 从 yml 移除，改 env 占位（`LEOMALL_WX_APP_SECRET`/`LEOMALL_COS_SECRET_ID`/`LEOMALL_COS_SECRET_KEY`）；DB url/账号/口令支持 `LEOMALL_MYSQL_URL/USER/PASSWORD` 覆盖（默认值仅本机开发）；cloud compose 与 `.env.example` 同步 | `application-core.yml`、`application-db.yml`、`deploy/cloud/docker-compose.yml`、`deploy/cloud/.env.example` |
| P1-8 | 导购聊天三层防护：每用户约 6 次/分钟 + 每日 100 次配额 + 每用户并发 ≤2（新增 `AgentChatGuard`，超限返回 803）；`AgentGateway` 全局在途 ≤20 + 连续失败 5 次熔断 30 秒，防 60 秒同步等待拖垮 Tomcat 线程 | 新增 `AgentChatGuard.java`、`AgentGateway.java`、`WxAgentController.java` |

**验证**：后端 `mvn compile` 7 个模块全部 BUILD SUCCESS（日志 `server/build-check.log`）；本轮未改动 Agent 代码，无需重跑 pytest。

**残留风险与运维动作**：
1. **存量微信用户**：历史上以 openId 明文入库的密码仍在库中，需一次性迁移（对 `username = weixin_openid` 的行重置为随机 BCrypt 值，或引导用户走短信重置）；git 历史中的 wx/COS 演示密钥与生产凭据轮换属运维动作，代码侧已不再引用。
2. 领券锁、短信频控、聊天限流/配额、熔断计数均为**单实例内存态**——多实例部署前必须迁 Redis（阶段 1），与既有会话/验证码/锁定/下单去重一致。
3. 短信图形验证码前置（人机校验）未实施，需 wx 端 kaptcha 端点 + H5 前端配套改造，列为阶段 1；当前以频率/次数风控兜底。
4. 聊天限流为同步阻塞模型下的保守阈值；异步化（WebClient/DeferredResult + 队列）属阶段 2 架构升级。
5. P3 工程项（CI 路径、`maven.test.skip` 全局跳过、qiniu 版本区间等）仍未修复。

## 八、阶段 1 修复记录：Redis 化（2026-11-24，已实施）

**设计**：新增 `StateStore` 抽象（`leomall-core/state`），按 `leomall.redis.enabled` 条件装配双实现——
`RedisStateStore`（StringRedisTemplate + Lua 原子计数/比较释放锁，全部键强制 TTL）与
`InMemoryStateStore`（默认降级，语义与 Redis 对齐）。**本机开发零 Redis 依赖不变**；
置 `LEOMALL_REDIS_ENABLED=true` 后下列组件全部跨实例共享，后端可水平扩展。

| 组件 | 迁移内容 | 涉及文件 |
|---|---|---|
| 基础设施 | starter-data-redis + commons-pool2；`spring.redis.*` 环境变量占位；Shiro 会话 JDK 序列化模板 | `leomall-core/pom.xml`、`StateStore.java`、`RedisStateStore.java`、`InMemoryStateStore.java`、`StateStoreConfiguration.java`、`application-core.yml` |
| 短信验证码 | 验证码 SETNX（天然 1 分钟间隔）+ TTL；失败计数/号码日限/IP 小时限改原子计数 | `CaptchaCodeManager.java`（改 Spring Bean）、`WxAuthController.java` |
| 管理端登录锁定 | 失败计数（15 分钟窗口随失败刷新）+ 锁定标志 TTL 15 分钟，跨实例共享 | `AdminLoginGuard.java` |
| 下单去重 | Guava Cache → SETNX 幂等键（userId+body MD5，TTL 10s），事务回滚释放 | `WxOrderService.java` |
| 领券/兑换锁 | Striped 进程锁 → 分布式锁（SETNX + token Lua 释放 + TTL 10s 兜底），最长等待 3s | `CouponClaimLock.java`、`WxCouponController.java` |
| 导购聊天守卫 | 每用户 6 次/分钟（固定窗口）+ 日配额 100 + 并发 ≤2，全部 Redis 计数；并发键 TTL 180s 自愈崩溃泄漏 | `AgentChatGuard.java` |
| Agent 网关熔断 | 失败计数/开路标志共享（任一实例触发全实例熔断 30s）；全局在途 Semaphore 保持进程内（本实例线程保护） | `AgentGateway.java` |
| 管理端会话 | 新增 `RedisSessionDAO`（键 `leomall:admin:session:{id}`，TTL=会话超时 6h、访问刷新，SCAN 遍历活跃会话）；ShiroConfig 条件装配并关闭定时校验扫描；kaptcha 跨实例可用 | 新增 `RedisSessionDAO.java`、`ShiroConfig.java` |
| 部署编排 | cloud compose：backend Redis 环境变量透传、agent `QUEUE_ENABLED/BREAKER_SHARED` 可配、新增 `agent-worker`（profile queue）；`.env.example` 阶段1 开关说明；nginx 多实例 upstream 注释模板 | `deploy/cloud/docker-compose.yml`、`.env.example`、`nginx/leomall.conf` |

**验证**：
- 后端 `mvn compile` / `mvn install` 7 模块全部 BUILD SUCCESS（含 war 打包）；
- 临时 Redis 7 容器真测：`InMemoryStateStore` 20 项、`RedisStateStore` 21 项、`RedisSessionDAO` 往返 5 项行为自检全部通过（TTL 过期、SETNX 互斥、原子计数并发精确、错误 token 不释放锁、锁 TTL 自动释放、会话属性序列化往返/更新/删除）；
- 自检发现并修复 1 处语义差异：Redis DECR 不存在键返回 -1，Lua 脚本补 EXISTS 前置判断对齐内存实现的 0；
- 本轮未改动 Agent Python 代码，pytest 基线不受影响。

**启用方式**：见 README「阶段1：Redis 共享状态」。云端 `docker compose --profile cache up -d --build`
（.env 设 `LEOMALL_REDIS_ENABLED=true`、`REDIS_URL=redis://redis:6379/0`）；队列削峰再加 `--profile queue` 与 `QUEUE_ENABLED=1`。

**残留与后续**：
1. **多实例的其他前置**：本地存储（`storage.active=local`）需改对象存储或共享卷（阶段 2）；未支付订单内存延迟任务保留为快路径，正确性由每 10 分钟 DB 扫描兜底（多实例重复触发幂等无害）；`HomeCacheManager`/`SystemConfig` 为只读缓存，进程内保留无害。
2. 聊天限流为固定窗口（边界处置最多 2 倍瞬时突发）；日配额检查-自增非严格原子，极端并发下可能少量超额，属软配额可接受。
3. Redis 本身为单点（compose 单容器）；生产高可用需哨兵/集群，连接参数已支持外部 Redis 直接接入。
4. 阶段 2（Boot 2.7→3.x/Java17、Shiro→Spring Security、Vue3、聊天异步化、对象存储）与 P3 工程项未开始。

## 九、P3 修复记录：工程质量（2026-11-24，已实施）

| # | 修复内容 | 涉及文件 |
|---|---|---|
| P3-1 | **重建 CI**（旧工作流此前已在工作区删除，本次为新建）：backend 在 `server/` 下 JDK 8 `mvn verify`；admin/h5 矩阵 Node 18 + `NODE_OPTIONS=--openssl-legacy-provider` 构建；agent 用 uv 锁定依赖 + compileall + 导入冒烟。旧 CI 的根目录 mvn（无 pom）、`leomall-admin/leomall-vue` 幽灵路径、Node 10/12/14 矩阵全部废弃 | `.github/workflows/main.yml`（新建） |
| P3-2 | 删除 `maven.test.skip=true`，**测试真正启用**；22 个环境依赖型遗留集成测试（需 MySQL/Redis/第三方密钥）由 surefire excludes 集中排除，`-Dtest=XxxTest` 可覆盖单跑；`DbUtilTest` 补 `throws IOException` 跟上 P2-5 新签名（excludes 只跳运行不跳编译） | `server/pom.xml`、`DbUtilTest.java` |
| P3-3 | qiniu 版本区间 `[7.2.0,7.2.99]` → 锁定 `7.2.29`，构建可复现 | `server/pom.xml` |
| P3-4 | devtools 从父 POM 公共依赖移入 `profile id=devtools`，默认构建/打包不含 | `server/pom.xml` |
| P3-5 | 20 处活跃 `e.printStackTrace()` 全部替换为 `logger.error(上下文, e)`（13 个文件；JwtHelper/TaskService/3 个 Json TypeHandler 原无 logger，补 commons-logging） | `WxOrderService`、`WxAuthController`、`WxGoodsController`、`WxMsgController`、`WxHomeController`、`JwtHelper`、`JacksonUtil`、`AliyunSmsSender`、`TaskService`、`JsonNodeTypeHandler`、`JsonIntegerArrayTypeHandler`、`JsonStringArrayTypeHandler` |
| P3-6 | history 不再全量加载后截断：新增 `queryRecentBySessionId(sessionId, limit)`（PageHelper SQL LIMIT，id desc 取最近 N 条再反转为升序），控制器只取最近 100 条 | `LeomallAgentMessageService.java`、`WxAgentController.java` |
| P3-7 | **未动**：miniprogram 归档属用户决定 | — |
| P3-8 | README 删除明文种子口令（admin123/user123），改为指向 `sql/leomall_schema.sql` 种子 + 部署前必须改密/删演示账号的警示 | `README.md` |
| P3-9 | 删除 powermock 1.6.6 / mockito 1.10.19（starter-test 自带 mockito 2.23）；`BCryptTest.testGensalt` 去 PowerMock 改盐格式断言；`BigDecimalTest` 改纯 JUnit 并补 compareTo 回归断言。mockito 5 / JUnit 5 迁移留阶段 2 | `server/pom.xml`、`BCryptTest.java`、`BigDecimalTest.java` |
| P3-10 | Agent 无 Redis 队列时同步直跑路径补**进程内 TTL 幂等**：指纹=会话+问句（与 `_enqueue` 复用同一函数），30 秒结果缓存——已完成重复直接返回原结果（不再重复执行含下单的写操作），处理中并发重复 409，失败释放登记允许重试；多副本部署仍需启用 Redis 队列 | `agent/app/presentation/server.py` |

**新增单元测试**（本轮起 CI 真实跑测）：

- 后端：`InMemoryStateStoreTest`（TTL/SETNX 互斥/原子计数/锁 token 语义/8 线程并发计数精确，5 项）、
  `RichTextUtilTest`（script/事件属性/iframe/javascript: 协议剔除、安全标签与 style 保留，4 项）、
  `IpUtilTest`（公网对端不可伪造头、回环/内网代理信任、unknown 回退，5 项）；
  加上改写的 `BCryptTest`（15）、`BigDecimalTest`（1）、`IntegerTest`（1），**共 31 项**。
- Agent：`tests/test_sync_idempotency.py`（结果复用/并发 409/失败释放/过期不 reuse/指纹会话隔离，5 项）。

**验证**：
- `mvn test` 与 `mvn verify`（与 CI 同命令）7 模块全部 BUILD SUCCESS，surefire 实跑 31/31 通过、0 失败，
  排除清单生效（无 SpringBootTest 被拉起）；日志 `server/build-check.log`。
- Agent pytest 全量：**276 过 / 15 失败，与基线完全一致**（无新增回归）；新增幂等测试 5/5 通过。
- CI YAML 语法校验通过；agent 导入冒烟（`import app.presentation.server`）本地等价验证通过；
  同步幂等逻辑另做行为自检（命中/409/失败释放）通过。

**残留与后续**：
1. Agent pytest 的 15 个存量失败（test_retrieval 8 / test_tools_and_eventbus 3 / test_usecases 4）源于
   夹具引用演示目录 P1001/P1006 但实际加载 leomall 快照；修好夹具后应把 pytest 纳入 CI 门禁。
2. 被排除的 22 个集成测试需在具备 MySQL/Redis/第三方密钥的环境手动运行；阶段 2 建议 JUnit 5 标签化
   （unit/integration）或 testcontainers 替代 surefire 排除清单。
3. CI 前端仅构建门禁（admin 无实际 jest 用例，lint 未纳入）；Node 18 构建依赖 openssl-legacy-provider，
   阶段 2 升 Vue3/Vite 后移除。
4. P3-7 miniprogram 归档待用户决定；CI 旧文件系工作区既有删除，本次重建为全新文件，提交时请一并纳入。

## 十、阶段 2A 记录：Boot 2.7.18 跳版 + 依赖对齐（2026-11-24，已实施）

阶段 2 按审计路线图（第三节升级顺序）拆子阶段推进：**2A Boot 2.7.18（本轮）** → 2B Java17 + Boot3 +
Spring Security → 2C 聊天异步化 → 2D Vue3 前端 → 2E 对象存储/可观测性。每步独立可回滚。

| 项 | 变更 | 涉及文件 |
|---|---|---|
| Boot 跳版 | 2.1.5.RELEASE → **2.7.18**（Java8 最后支持版，javax 保留）；compiler 插件交父 POM 管理（3.10.1） | `server/pom.xml` |
| MyBatis/PageHelper | starter 1.3.2 → 2.3.2；pagehelper 1.2.5 → 1.4.7 | `server/pom.xml` |
| 连接池/驱动 | druid 1.2.1 → 1.2.23；`mysql:mysql-connector-java:8.0.28` → `com.mysql:mysql-connector-j:8.4.0`（GA 新坐标，含 generator 插件依赖） | `server/pom.xml`、`leomall-db/pom.xml` |
| 微信 SDK | weixin-java-pay/miniapp 4.1.0 → 4.6.0（支付解析与证书处理修复；API 兼容，编译+运行时冒烟验证） | `server/pom.xml` |
| 对象存储 SDK | aliyun-sdk-oss 2.5.0 → 3.17.4；`AliyunStorage` 改 `OSSClientBuilder` 构造（旧构造器废弃） | `server/pom.xml`、`AliyunStorage.java` |
| guava | 32.0.0-jre → 33.4.8-jre | `server/pom.xml` |
| 版本管理 | hibernate-validator / starter-json / starter-mail 移除硬编码版本，交 Boot BOM 管理 | `server/pom.xml` |
| **springfox → springdoc** | springfox 2.9.2/2.10.0 + swagger-bootstrap-ui 1.9.6 全部移除（与 Boot 2.6+ 不兼容且停维）→ springdoc-openapi-ui 1.7.0；两套 Docket 配置改 GroupedOpenApi（admin/wx 分组保留）；3 处死导入 `io.swagger.models.auth.In` 清除 | 两套 `*Swagger2Configuration.java`、admin/wx/all 三个 `application.yml`、3 个 Controller |
| 文档开关 | P2-6 开关语义保留但更名：`swagger.production` → `SWAGGER_ENABLED`（springdoc.api-docs.enabled/swagger-ui.enabled 同源），生产设 false 时自动配置与分组 Bean 全关 | 三个 `application.yml`、两套配置类 |
| 循环依赖过渡 | Boot 2.6+ 默认禁止循环引用；`spring.main.allow-circular-references=true` 过渡（application-core.yml），**阶段2B 逐个解环后移除** | `application-core.yml` |
| JUnit4 续跑 | Boot 2.7 starter-test 默认仅 JUnit5；补 junit-vintage-engine（test）使现存 31 项 JUnit4 测试继续运行（阶段2B 迁 JUnit5 后移除） | `server/pom.xml` |
| API 适配 | Spring Data Redis 2.7 的 `Cursor.close()` 不再抛受检 IOException，移除不可达 catch | `RedisSessionDAO.java` |
| 打包路径 | all/war 模块前端资源目录 `../leomall-admin/dist`、`../leomall-vue/dist`（幽灵路径）→ `../admin/dist`、`../h5/dist` | `leomall-all/pom.xml`、`leomall-all-war/pom.xml` |

**验证（本轮首次做到运行时级）**：
- `mvn verify` 7 模块 BUILD SUCCESS，31/31 测试通过（vintage 引擎生效）。
- **临时 MySQL 8.0 容器**（端口 13306，导入 schema/table/data 共 37 表）+ 真实启动 `leomall-all` exec jar：
  - 内存态：启动 15s（Boot 2.7.18 / Spring 5.3.31）；`wx/home/index` errno=0、`wx/goods/list` total=238
    （MyBatis 2.3 + PageHelper 1.4.7 + connector-j 8.4 集成验证）；kaptcha 200；无验证码登录被拒 errno=607；
    `/v3/api-docs/admin` openapi 3.0.1 / 133 paths；swagger-ui 200。
  - Redis 态（复用本机 Redis 7，db15 隔离）：启动 13s；Shiro 会话键 `leomall:admin:session:{id}` 落 Redis；
    错误验证码登录 errno=606（跨实例会话校验链路通）；`SWAGGER_ENABLED=false` 时 `/v3/api-docs/admin` **404**。
  - wxjava 4.6 运行时：伪造 code 登录返回结构化 errno=401，无异常泄漏。
- 冒烟后已清理：应用进程停止、MySQL 容器删除、db15 测试键清空、临时日志删除。

**残留与后续（阶段2B 输入）**：
1. `allow-circular-references=true` 为过渡开关，需定位并解开字段注入环（构造注入/事件解耦）。
2. Shiro 1.13 仍在（javax）；阶段2B 换 Spring Security 6 时统一路径匹配语义（当前 MVC 已用 PathPatternParser，
   springdoc 兼容，未走 ant_path_matcher 回头路）。
3. javax→jakarta 全量迁移、Boot 3.x、Java 17 编译目标、springdoc 2.x、JUnit5 + mockito5、vintage 移除。
4. 前端 Vue2/webpack4 未动（CI 仍靠 openssl-legacy-provider）；阶段2D 迁 Vue3 + Vite。
5. qiniu 7.2.29 / cos_api / qcloudsms 等本轮未动，随 2B/2E 评估。
6. 运维注意：生产部署参数 `swagger.production=true` 需改为 `SWAGGER_ENABLED=false`。