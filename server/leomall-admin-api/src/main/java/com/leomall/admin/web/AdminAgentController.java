package com.leomall.admin.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.leomall.admin.annotation.RequiresPermissionsDesc;
import com.leomall.core.util.ResponseUtil;
import com.leomall.core.validator.Order;
import com.leomall.core.validator.Sort;
import com.leomall.db.dao.AgentStatMapper;
import com.leomall.db.domain.LeomallAgentRecommendation;
import com.leomall.db.service.LeomallAgentMessageService;
import com.leomall.db.service.LeomallAgentRecommendationService;
import com.leomall.db.service.LeomallAgentSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端：智能导购会话记录 / 消息详情 / 推荐效果。
 *
 * 按用户要求：对话内容不脱敏、不设置保留期限、不限制查看与导出。
 * 数据在 BFF 侧双写进 leomall MySQL，管理端无需跨库读 Agent 的 SQLite。
 */
@RestController
@RequestMapping("/leomall/admin/agent")
public class AdminAgentController {

    private final Log logger = LogFactory.getLog(AdminAgentController.class);

    @Autowired
    private LeomallAgentSessionService sessionService;
    @Autowired
    private LeomallAgentMessageService messageService;
    @Autowired
    private LeomallAgentRecommendationService recommendationService;
    @Autowired
    private AgentStatMapper agentStatMapper;

    @RequiresPermissions("admin:agent:list")
    @RequiresPermissionsDesc(menu = {"智能助手", "会话记录"}, button = "查询")
    @GetMapping("/sessions")
    public Object sessions(String userId, String keyword,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "20") Integer limit,
                           @Sort @RequestParam(defaultValue = "add_time") String sort,
                           @Order @RequestParam(defaultValue = "desc") String order) {
        return ResponseUtil.okList(sessionService.querySelective(userId, keyword, page, limit, sort, order));
    }

    @RequiresPermissions("admin:agent:read")
    @RequiresPermissionsDesc(menu = {"智能助手", "会话记录"}, button = "详情")
    @GetMapping("/messages")
    public Object messages(String sessionId, String userId,
                           @RequestParam(defaultValue = "1") Integer page,
                           @RequestParam(defaultValue = "100") Integer limit,
                           @Sort @RequestParam(defaultValue = "add_time") String sort,
                           @Order @RequestParam(defaultValue = "asc") String order) {
        return ResponseUtil.okList(messageService.querySelective(sessionId, userId, page, limit, sort, order));
    }

    @RequiresPermissions("admin:agent:read")
    @RequiresPermissionsDesc(menu = {"智能助手", "会话记录"}, button = "推荐明细")
    @GetMapping("/recommendations")
    public Object recommendations(String sessionId, Integer userId,
                                  @RequestParam(defaultValue = "1") Integer page,
                                  @RequestParam(defaultValue = "100") Integer limit) {
        List<LeomallAgentRecommendation> rows =
                recommendationService.querySelective(sessionId, userId, page, limit);
        // rank 是 MySQL 8 保留字，列名用 sort_rank；这里按前端契约映射回 rank
        List<Map<String, Object>> list = new ArrayList<>();
        for (LeomallAgentRecommendation r : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("messageId", r.getMessageId());
            item.put("sessionId", r.getSessionId());
            item.put("userId", r.getUserId());
            item.put("goodsId", r.getGoodsId());
            item.put("goodsName", r.getGoodsName());
            item.put("goodsPic", r.getGoodsPic());
            item.put("retailPrice", r.getRetailPrice());
            item.put("score", r.getScore());
            item.put("rank", r.getSortRank());
            item.put("clicked", r.getClicked());
            item.put("addTime", r.getAddTime());
            list.add(item);
        }
        // 手动映射后不能依赖 PageHelper 的 Page 类型补 total，这里走 okList(list)；
        // 前端详情弹窗展示全量（limit=100），总数字段由下方 stats 提供。
        return ResponseUtil.okList(list);
    }

    @RequiresPermissions("admin:agent:stats")
    @RequiresPermissionsDesc(menu = {"智能助手", "推荐效果"}, button = "查询")
    @GetMapping("/stats")
    public Object stats(@RequestParam(defaultValue = "10") Integer topLimit) {
        long sessionCount = sessionService.countAll();
        long messageCount = messageService.countAll();
        long recommendCount = recommendationService.countAll();
        long clickCount = recommendationService.countClicked();
        long userCount = agentStatMapper.countAgentUsers();

        List<Map<String, Object>> topGoods = new ArrayList<>();
        for (Map<String, Object> row : agentStatMapper.topRecommendGoods(Math.min(Math.max(topLimit, 1), 50))) {
            Map<String, Object> item = new HashMap<>();
            item.put("goodsId", pick(row, "goodsId", "goodsid"));
            item.put("goodsName", pick(row, "goodsName", "goodsname"));
            item.put("count", pick(row, "cnt", "count"));
            topGoods.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sessionCount", sessionCount);
        data.put("messageCount", messageCount);
        data.put("userCount", userCount);
        data.put("recommendCount", recommendCount);
        data.put("clickCount", clickCount);
        data.put("clickRate", recommendCount == 0 ? 0d : (double) clickCount / recommendCount);
        data.put("topGoods", topGoods);
        return ResponseUtil.ok(data);
    }

    /** MySQL 驱动对不同版本的大小写处理不一致，聚合列按多种写法兜底。 */
    private static Object pick(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
