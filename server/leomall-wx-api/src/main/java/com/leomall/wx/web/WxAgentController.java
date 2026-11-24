package com.leomall.wx.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.leomall.core.util.JacksonUtil;
import com.leomall.core.util.ResponseUtil;
import com.leomall.db.domain.LeomallAgentMessage;
import com.leomall.db.domain.LeomallAgentRecommendation;
import com.leomall.db.domain.LeomallAgentSession;
import com.leomall.db.domain.LeomallGoods;
import com.leomall.db.service.LeomallAgentMessageService;
import com.leomall.db.service.LeomallAgentRecommendationService;
import com.leomall.db.service.LeomallAgentSessionService;
import com.leomall.db.service.LeomallGoodsService;
import com.leomall.wx.annotation.LoginUser;
import com.leomall.wx.service.AgentChatGuard;
import com.leomall.wx.service.AgentGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 智能导购（H5 用户端）接口。
 *
 * 链路：H5 带 X-Leomall-Token 提问 → @LoginUser 解析 userId → AgentGateway 注入 buyer_id
 * 转发 Python Agent → 返回的 goods_id 逐条用 LeomallGoodsService 校验在售 →
 * 会话 / 消息 / 推荐写 MySQL → 返回真实商品卡。
 *
 * 前端不能传 buyer_id；所有会话查询都按 userId 隔离，避免越权读取他人对话。
 */
@RestController
@RequestMapping("/leomall/wx/agent")
public class WxAgentController {

    private final Log logger = LogFactory.getLog(WxAgentController.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_HISTORY = 100;

    @Autowired
    private AgentGateway agentGateway;
    @Autowired
    private AgentChatGuard agentChatGuard;
    @Autowired
    @Qualifier("agentChatExecutor")
    private ThreadPoolExecutor agentChatExecutor;
    @Value("${leomall.agent.chat.async-timeout-ms:55000}")
    private long chatAsyncTimeoutMs;
    @Autowired
    private LeomallAgentSessionService sessionService;
    @Autowired
    private LeomallAgentMessageService messageService;
    @Autowired
    private LeomallAgentRecommendationService recommendationService;
    @Autowired
    private LeomallGoodsService goodsService;

    /**
     * 一轮对话：提问 → 推荐 → 落库 → 返回商品卡。
     *
     * 阶段2C（P1-8 异步化）：返回 DeferredResult，阻塞部分（Agent 调用 + 校验落库）
     * 移入独立有界线程池 agentChatExecutor，Tomcat 线程即时释放。
     * HTTP 契约不变（同一 POST、同一 JSON 结构），H5 前端零改动。
     * 快速失败路径：未登录 501 / 参数 401 / 限流 803 / 池满 802 / 异步超时 802。
     */
    @PostMapping("chat")
    public DeferredResult<Object> chat(@LoginUser Integer userId, @RequestBody String body) {
        DeferredResult<Object> result = new DeferredResult<>(chatAsyncTimeoutMs);
        if (userId == null) {
            result.setResult(ResponseUtil.unlogin());
            return result;
        }
        String query = JacksonUtil.parseString(body, "query");
        if (StringUtils.isEmpty(query) || query.trim().isEmpty()) {
            result.setResult(ResponseUtil.badArgument());
            return result;
        }
        final String trimmedQuery = query.trim().length() > MAX_QUERY_LENGTH
                ? query.trim().substring(0, MAX_QUERY_LENGTH) : query.trim();

        // P1-8：每用户限流（约6次/分钟）+ 每日配额（100次）+ 并发上限（2），
        // 防 LLM 成本滥用。在请求线程完成，拒绝时不占用线程池。
        String reject = agentChatGuard.tryAcquire(userId);
        if (reject != null) {
            result.setResult(ResponseUtil.fail(803, reject));
            return result;
        }

        final String requestedSessionId = JacksonUtil.parseString(body, "sessionId");
        // 异步超时：任务可能仍在跑（由其 finally 释放守卫）或仍在排队（执行后释放），
        // 这里只负责给客户端一个确定的失败响应；setResult 幂等，后到的任务结果被丢弃。
        result.onTimeout(() -> {
            logger.warn("导购聊天异步超时（" + chatAsyncTimeoutMs + "ms），userId=" + userId);
            result.setResult(ResponseUtil.fail(802, "智能助手响应超时，请稍后重试"));
        });

        try {
            agentChatExecutor.execute(() -> {
                try {
                    result.setResult(doChat(userId, requestedSessionId, trimmedQuery));
                } catch (Exception e) {
                    logger.error("导购聊天处理失败，userId=" + userId, e);
                    result.setResult(ResponseUtil.serious());
                } finally {
                    agentChatGuard.release(userId);
                }
            });
        } catch (RejectedExecutionException rex) {
            // 池满且队列满：快速失败，不排队
            agentChatGuard.release(userId);
            result.setResult(ResponseUtil.fail(802, "智能助手繁忙，请稍后重试"));
        }
        return result;
    }

    /** 聊天重活（在 agentChatExecutor 线程执行）：会话 → Agent → 校验落库 → 商品卡。 */
    private Object doChat(Integer userId, String requestedSessionId, String query) {
        String sessionId = requestedSessionId;
        LeomallAgentSession session = null;
        if (!StringUtils.isEmpty(sessionId)) {
            // 会话必须属于当前登录用户，避免越权读取他人对话
            session = sessionService.findBySessionIdAndUser(sessionId, userId);
        }
        if (session == null) {
            // 浏览器可能残留已失效或属于其他登录账号的会话号。
            // 不沿用、也不报 506，直接为当前用户开启新会话，避免前端反复提示"无操作权限"。
            sessionId = "lt-" + userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            session = new LeomallAgentSession();
            session.setUserId(userId);
            session.setSessionId(sessionId);
            session.setTitle(truncate(query, 120));
            session.setTurnCount(0);
            session.setLastQuery("");
            session.setStatus(0);
            session.setModel("");
            sessionService.add(session);
        }

        long started = System.currentTimeMillis();
        Map<String, Object> reply;
        try {
            reply = agentGateway.chat(sessionId, userId, query);
        } catch (Exception e) {
            logger.error("调用智能导购 Agent 失败", e);
            return ResponseUtil.fail(802, "智能助手暂时不可用，请稍后重试");
        }
        int latencyMs = (int) (System.currentTimeMillis() - started);

        // 用户消息
        LeomallAgentMessage userMessage = new LeomallAgentMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setRole("user");
        userMessage.setContent(query);
        userMessage.setModel("");
        userMessage.setLatencyMs(0);
        userMessage.setDegraded(false);
        messageService.add(userMessage);

        // Agent 回复
        String model = str(reply.get("model"));
        boolean degraded = Boolean.TRUE.equals(reply.get("degraded"));
        LeomallAgentMessage assistantMessage = new LeomallAgentMessage();
        assistantMessage.setSessionId(sessionId);
        assistantMessage.setUserId(userId);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(truncate(str(reply.get("finalText")), 2000));
        assistantMessage.setModel(truncate(model, 60));
        assistantMessage.setLatencyMs(latencyMs);
        assistantMessage.setDegraded(degraded);
        messageService.add(assistantMessage);
        Integer messageId = assistantMessage.getId();

        // 推荐商品：一律用主库校验，防止 Agent 幻觉 / 下架商品 / 快照价格滞后
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawItems =
                (List<Map<String, Object>>) reply.getOrDefault("recommendations", new ArrayList<Map<String, Object>>());
        List<Map<String, Object>> cards = new ArrayList<>();
        int rank = 0;
        for (Map<String, Object> raw : rawItems) {
            Object idValue = raw.get("goodsId");
            Integer goodsId = idValue instanceof Number ? ((Number) idValue).intValue() : null;
            if (goodsId == null || goodsId <= 0) {
                continue;
            }
            LeomallGoods goods = goodsService.findById(goodsId);
            if (goods == null || !Boolean.TRUE.equals(goods.getIsOnSale())
                    || Boolean.TRUE.equals(goods.getDeleted())) {
                continue;
            }
            rank++;
            LeomallAgentRecommendation record = new LeomallAgentRecommendation();
            record.setMessageId(messageId);
            record.setSessionId(sessionId);
            record.setUserId(userId);
            record.setGoodsId(goodsId);
            record.setGoodsName(truncate(goods.getName(), 120));
            record.setGoodsPic(truncate(goods.getPicUrl(), 250));
            record.setRetailPrice(goods.getRetailPrice());
            record.setScore(BigDecimal.valueOf(doubleValue(raw.get("score"))));
            record.setSortRank(rank);
            record.setClicked(false);
            recommendationService.add(record);

            Map<String, Object> card = new HashMap<>();
            card.put("recommendationId", record.getId());
            card.put("goodsId", goodsId);
            card.put("name", goods.getName());
            card.put("brief", goods.getBrief());
            card.put("picUrl", goods.getPicUrl());
            card.put("retailPrice", goods.getRetailPrice());
            card.put("counterPrice", goods.getCounterPrice());
            card.put("score", doubleValue(raw.get("score")));
            card.put("rank", rank);
            cards.add(card);
        }

        // 更新会话摘要
        LeomallAgentSession patch = new LeomallAgentSession();
        patch.setId(session.getId());
        patch.setTurnCount((session.getTurnCount() == null ? 0 : session.getTurnCount()) + 1);
        patch.setLastQuery(truncate(query, 250));
        patch.setModel(truncate(model, 60));
        sessionService.updateById(patch);

        Map<String, Object> data = new HashMap<>();
        data.put("sessionId", sessionId);
        data.put("messageId", messageId);
        data.put("reply", assistantMessage.getContent());
        data.put("recommendations", cards);
        data.put("latencyMs", latencyMs);
        data.put("model", model);
        data.put("degraded", degraded);
        data.put("addTime", formatTime(assistantMessage.getAddTime()));
        return ResponseUtil.ok(data);
    }

    /** 当前用户的会话列表（按 userId 隔离）。 */
    @GetMapping("sessions")
    public Object sessions(@LoginUser Integer userId) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (LeomallAgentSession s : sessionService.queryByUid(userId)) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", s.getId());
            item.put("sessionId", s.getSessionId());
            item.put("title", s.getTitle());
            item.put("turnCount", s.getTurnCount());
            item.put("lastQuery", s.getLastQuery());
            item.put("status", s.getStatus());
            item.put("addTime", formatTime(s.getAddTime()));
            list.add(item);
        }
        return ResponseUtil.ok(list);
    }

    /** 单个会话的逐轮消息与推荐商品。 */
    @GetMapping("history")
    public Object history(@LoginUser Integer userId, String sessionId) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        if (StringUtils.isEmpty(sessionId)) {
            return ResponseUtil.badArgument();
        }
        LeomallAgentSession session = sessionService.findBySessionIdAndUser(sessionId, userId);
        if (session == null) {
            return ResponseUtil.unauthz();
        }
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("sessionId", session.getSessionId());
        sessionMap.put("title", session.getTitle());
        sessionMap.put("turnCount", session.getTurnCount());
        sessionMap.put("addTime", formatTime(session.getAddTime()));

        List<Map<String, Object>> messages = new ArrayList<>();
        // P3-6：SQL 层只取最近 MAX_HISTORY 条（升序返回），不再全量加载后截断
        List<LeomallAgentMessage> rows = messageService.queryRecentBySessionId(sessionId, MAX_HISTORY);
        for (LeomallAgentMessage m : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            item.put("model", m.getModel());
            item.put("latencyMs", m.getLatencyMs());
            item.put("degraded", m.getDegraded());
            item.put("addTime", formatTime(m.getAddTime()));
            item.put("recommendations", cardsByMessageId(m.getId()));
            messages.add(item);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("session", sessionMap);
        data.put("messages", messages);
        return ResponseUtil.ok(data);
    }

    /** 用户点「新会话」：只标记旧会话结束，不删除记录（管理端保留全量会话）。 */
    @PostMapping("clear")
    public Object clear(@LoginUser Integer userId, @RequestBody String body) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        String sessionId = JacksonUtil.parseString(body, "sessionId");
        if (!StringUtils.isEmpty(sessionId)) {
            LeomallAgentSession session = sessionService.findBySessionIdAndUser(sessionId, userId);
            if (session != null) {
                LeomallAgentSession patch = new LeomallAgentSession();
                patch.setId(session.getId());
                patch.setStatus(1);
                sessionService.updateById(patch);
            }
        }
        return ResponseUtil.ok();
    }

    /** 商品卡点击埋点，用于管理端推荐效果统计。 */
    @PostMapping("click")
    public Object click(@LoginUser Integer userId, @RequestBody String body) {
        if (userId == null) {
            return ResponseUtil.unlogin();
        }
        Integer recommendationId = JacksonUtil.parseInteger(body, "recommendationId");
        if (recommendationId == null) {
            return ResponseUtil.badArgument();
        }
        recommendationService.markClicked(recommendationId, userId);
        return ResponseUtil.ok();
    }

    // ---- helpers ----

    private List<Map<String, Object>> cardsByMessageId(Integer messageId) {
        List<Map<String, Object>> cards = new ArrayList<>();
        if (messageId == null) {
            return cards;
        }
        for (LeomallAgentRecommendation r : recommendationService.queryByMessageId(messageId)) {
            Map<String, Object> card = new HashMap<>();
            card.put("recommendationId", r.getId());
            card.put("goodsId", r.getGoodsId());
            card.put("name", r.getGoodsName());
            card.put("brief", "");
            card.put("picUrl", r.getGoodsPic());
            card.put("retailPrice", r.getRetailPrice());
            card.put("counterPrice", null);
            card.put("score", r.getScore());
            card.put("rank", r.getSortRank());
            card.put("clicked", r.getClicked());
            card.put("addTime", formatTime(r.getAddTime()));
            cards.add(card);
        }
        return cards;
    }

    private static String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static double doubleValue(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
