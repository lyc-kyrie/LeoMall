package com.leomall.wx.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.leomall.core.util.JacksonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 调用外部 Python Agent 服务的 BFF 网关。
 *
 * 项目当前没有任何 HTTP 客户端依赖（无 RestTemplate / OkHttp / WebFlux），
 * 这里用 JDK 8 原生 HttpURLConnection，避免引入与 Servlet 栈冲突的依赖。
 * 服务只监听 127.0.0.1，且用 X-Internal-Token 做服务间鉴权。
 *
 * 安全约定：buyer_id 由本类从 @LoginUser 解析出的 userId 注入，
 * 绝不接受前端传入，避免用户伪造他人身份。
 */
@Component
public class AgentGateway {

    private final Log logger = LogFactory.getLog(AgentGateway.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${leomall.agent.base-url:http://127.0.0.1:8000}")
    private String baseUrl;

    @Value("${leomall.agent.internal-token:}")
    private String internalToken;

    @Value("${leomall.agent.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${leomall.agent.read-timeout-ms:60000}")
    private int readTimeoutMs;

    // P1-8 / 阶段1：全局在途上限（本实例线程保护，保持进程内）+ 熔断状态共享。
    // 熔断计数/开路标志走 StateStore：Redis 启用后任一实例触发熔断，
    // 所有实例同时快速失败，避免故障期间继续向 Agent 压请求。
    private static final int MAX_INFLIGHT = 20;
    private static final int BREAKER_FAILURE_THRESHOLD = 5;
    private static final String BREAKER_FAIL_KEY = "leomall:agent:breaker:fail";
    private static final String BREAKER_OPEN_KEY = "leomall:agent:breaker:open";
    private final java.util.concurrent.Semaphore inflight = new java.util.concurrent.Semaphore(MAX_INFLIGHT);

    @org.springframework.beans.factory.annotation.Autowired
    private com.leomall.core.state.StateStore stateStore;

    /** 返回已规范化的回复：sessionId / finalText / recommendations / model / degraded / recallStrategy。 */
    public Map<String, Object> chat(String sessionId, Integer userId, String rawQuery) throws IOException {
        ensureAvailable();
        try {
            String response = postJson("/commerce/intents", buildBody(sessionId, userId, rawQuery));
            Map<String, Object> result = normalizeReply(MAPPER.readTree(response), sessionId, response);
            stateStore.delete(BREAKER_FAIL_KEY);
            return result;
        } catch (IOException | RuntimeException e) {
            recordFailure();
            throw e;
        } finally {
            inflight.release();
        }
    }

    /**
     * SSE 流式调用（2026-11-25）：delta 事件把原始 JSON 串（{"token": "..."}）逐段回调 onDelta，
     * final 事件给出与 {@link #chat} 相同结构的规范化结果。
     * 买家端断开（onDelta 抛 {@link ClientDisconnectedException}）不计入熔断。
     */
    public Map<String, Object> chatStream(String sessionId, Integer userId, String rawQuery,
                                          Consumer<String> onDelta) throws IOException {
        ensureAvailable();
        HttpURLConnection conn = null;
        try {
            conn = openPost("/commerce/intents/stream", buildBody(sessionId, userId, rawQuery), "text/event-stream");
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("Agent 服务 HTTP " + code + "：" + abbreviate(readAll(conn.getErrorStream())));
            }
            Map<String, Object> result = null;
            String streamError = null;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                String eventName = "";
                StringBuilder data = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) {
                        if (data.length() > 0) {
                            String payload = data.toString();
                            if ("delta".equals(eventName)) {
                                onDelta.accept(payload);
                            } else if ("final".equals(eventName)) {
                                result = normalizeReply(MAPPER.readTree(payload), sessionId, payload);
                            } else if ("error".equals(eventName)) {
                                streamError = payload;
                            }
                        }
                        eventName = "";
                        data.setLength(0);
                    } else if (line.startsWith("event:")) {
                        eventName = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        data.append(line.substring(5).trim());
                    }
                }
            }
            if (streamError != null) {
                throw new IOException("Agent 流式处理失败：" + abbreviate(streamError));
            }
            if (result == null) {
                throw new IOException("Agent 流未返回最终结果");
            }
            stateStore.delete(BREAKER_FAIL_KEY);
            return result;
        } catch (ClientDisconnectedException e) {
            // 买家端断开不是网关故障：不计熔断，直接上抛由控制器收尾
            throw e;
        } catch (IOException | RuntimeException e) {
            recordFailure();
            throw e;
        } finally {
            inflight.release();
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 买家端 SSE 连接已断开（控制器 send 失败时抛出），用于熔断豁免。 */
    public static class ClientDisconnectedException extends RuntimeException {
        public ClientDisconnectedException(Throwable cause) {
            super(cause);
        }
    }

    private void ensureAvailable() throws IOException {
        if (stateStore.get(BREAKER_OPEN_KEY) != null) {
            throw new IOException("Agent 服务熔断中，暂时不可用");
        }
        if (!inflight.tryAcquire()) {
            throw new IOException("Agent 服务繁忙，请稍后重试");
        }
    }

    private void recordFailure() {
        if (stateStore.increment(BREAKER_FAIL_KEY, java.time.Duration.ofSeconds(60))
                >= BREAKER_FAILURE_THRESHOLD) {
            stateStore.set(BREAKER_OPEN_KEY, "1", java.time.Duration.ofSeconds(30));
            stateStore.delete(BREAKER_FAIL_KEY);
            logger.warn("Agent 连续失败达到阈值，熔断 30 秒");
        }
    }

    private String buildBody(String sessionId, Integer userId, String rawQuery) {
        Map<String, Object> body = new HashMap<>();
        body.put("shopping_session_id", sessionId);
        body.put("buyer_id", String.valueOf(userId));
        body.put("locale", "zh-CN");
        body.put("currency", "CNY");
        body.put("raw_query", rawQuery);
        return JacksonUtil.toJson(body);
    }

    private Map<String, Object> normalizeReply(JsonNode root, String sessionId, String rawForError)
            throws IOException {
        if (root == null || !root.isObject()) {
            throw new IOException("Agent 服务返回格式异常：" + abbreviate(rawForError));
        }
        if (root.hasNonNull("detail") && !root.has("final_text")) {
            throw new IOException("Agent 服务拒绝请求：" + root.get("detail").asText());
        }
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", text(root, "shopping_session_id", sessionId));
        result.put("finalText", text(root, "final_text", ""));
        result.put("model", text(root, "model", ""));
        result.put("degraded", root.path("degraded").asBoolean(false));
        result.put("recallStrategy", text(root, "recall_strategy", ""));
        result.put("recommendations", parseRecommendations(root.path("recommendations")));
        return result;
    }

    private List<Map<String, Object>> parseRecommendations(JsonNode array) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (array == null || !array.isArray()) {
            return items;
        }
        for (JsonNode node : array) {
            // 产品决策 2026-11-25：推荐上限 4（Python 侧已截断，此处双保险）
            if (items.size() >= 4) {
                break;
            }
            int goodsId = node.path("goods_id").asInt(0);
            if (goodsId <= 0) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("goodsId", goodsId);
            item.put("rank", node.path("rank").asInt(items.size() + 1));
            item.put("score", node.path("score").asDouble(0));
            item.put("name", text(node, "name", ""));
            item.put("brief", text(node, "brief", ""));
            item.put("picUrl", text(node, "pic_url", ""));
            item.put("retailPrice", node.path("retail_price").asDouble(0));
            item.put("counterPrice", node.path("counter_price").asDouble(0));
            item.put("categoryName", text(node, "category_name", ""));
            items.add(item);
        }
        return items;
    }

    private String postJson(String path, String json) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = openPost(path, json, "application/json");
            int code = conn.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String payload = readAll(stream);
            if (code < 200 || code >= 300) {
                throw new IOException("Agent 服务 HTTP " + code + "：" + abbreviate(payload));
            }
            return payload;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /** 建立 POST 连接并写出请求体（不读响应）；流式路径需要保持连接逐行读 SSE。 */
    private HttpURLConnection openPost(String path, String json, String accept) throws IOException {
        URL url = new URL(baseUrl.trim().replaceAll("/+$", "") + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(connectTimeoutMs);
        conn.setReadTimeout(readTimeoutMs);
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setRequestProperty("Accept", accept);
        if (internalToken != null && !internalToken.trim().isEmpty()) {
            conn.setRequestProperty("X-Internal-Token", internalToken.trim());
        }
        try (OutputStream out = conn.getOutputStream()) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() > 200 ? value.substring(0, 200) + "..." : value;
    }
}
