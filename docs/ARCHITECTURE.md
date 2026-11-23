# LeoMall 架构

```
H5 (h5/, :6255)  --/leomall/wx/**--+
                                   +-> Spring Boot (server/, :8080) -> MySQL leomall
管理后台 (admin/, :9527) --/leomall/admin/**-+        |
                                                     | X-Internal-Token
                                          智能导购 Agent (agent/, :8000)
```

- Java 后端作为 BFF：`@LoginUser` 注入用户身份，前端不可伪造 `buyer_id`
- Agent 返回 `goods_id` 后，BFF 复核商品在售且未删除，才组装商品卡
- 会话、消息、推荐记录写入 `leomall_agent_session / _message / _recommendation`
- Agent 自身 SQLite 只保存多轮状态，不作为管理端数据源
- 管理后台提供会话记录、消息详情、推荐明细与推荐效果统计

## 模块依赖

`leomall-core` <- `leomall-db` <- `leomall-admin-api` / `leomall-wx-api` <- `leomall-all`

## 关键约定

- Java 包名 `com.leomall.*`，数据库表前缀 `leomall_`
- REST 前缀 `/leomall/admin`、`/leomall/wx`
- Shiro 权限串由接口路径与 `@RequiresPermissionsDesc` 生成
