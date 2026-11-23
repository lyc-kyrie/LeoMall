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

    /** 返回已规范化的回复：sessionId / finalText / recommendations / model / degraded / recallStrategy。 */
    public Map<String, Object> chat(String sessionId, Integer userId, String rawQuery) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("shopping_session_id", sessionId);
        body.put("buyer_id", String.valueOf(userId));
        body.put("locale", "zh-CN");
        body.put("currency", "CNY");
        body.put("raw_query", rawQuery);

        String response = postJson("/commerce/intents", JacksonUtil.toJson(body));
        JsonNode root = MAPPER.readTree(response);
        if (root == null || !root.isObject()) {
            throw new IOException("Agent 服务返回格式异常：" + abbreviate(response));
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
            URL url = new URL(baseUrl.trim().replaceAll("/+$", "") + path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            if (internalToken != null && !internalToken.trim().isEmpty()) {
                conn.setRequestProperty("X-Internal-Token", internalToken.trim());
            }
            try (OutputStream out = conn.getOutputStream()) {
                out.write(json.getBytes(StandardCharsets.UTF_8));
            }
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
