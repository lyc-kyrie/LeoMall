package com.leomall.wx.service;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.leomall.core.state.StateStore;

/**
 * P1-8 / 阶段1：智能导购聊天的每用户流量守卫。
 *
 * 每分钟 ≤6 次（固定窗口）+ 每日 100 次配额 + 每用户并发在途 ≤2，
 * 防 LLM 成本滥用与长请求占满 Tomcat 线程。
 * StateStore 支撑：Redis 启用后限额跨实例共享（多实例下同一用户
 * 无法通过轮询实例翻倍配额）；未启用时降级为进程内计数。
 *
 * 说明：固定窗口在相邻窗口边界处置最多可能出现 2 倍瞬时突发，
 * 属可接受的保守限流；并发计数带 TTL 自愈，实例崩溃不会永久泄漏。
 */
@Component
public class AgentChatGuard {

    private static final int RATE_LIMIT_PER_MINUTE = 6;
    private static final int DAILY_LIMIT = 100;
    private static final int MAX_CONCURRENT_PER_USER = 2;

    private final StateStore state;

    public AgentChatGuard(StateStore state) {
        this.state = state;
    }

    private static String dailyKey(Integer userId, long epochDay) {
        return "leomall:agent:chat:daily:" + userId + ":" + epochDay;
    }

    private static String inflightKey(Integer userId) {
        return "leomall:agent:chat:inflight:" + userId;
    }

    /**
     * 尝试获取一次聊天许可。
     *
     * @return null 表示放行（调用方结束后必须 release）；否则返回拒绝原因
     */
    public String tryAcquire(Integer userId) {
        long today = LocalDate.now().toEpochDay();
        String dailyUsed = state.get(dailyKey(userId, today));
        if (dailyUsed != null && Long.parseLong(dailyUsed) >= DAILY_LIMIT) {
            return "今日智能导购次数已用完，请明天再来";
        }

        long minuteBucket = System.currentTimeMillis() / 60_000L;
        long minuteCount = state.increment(
                "leomall:agent:chat:rate:" + userId + ":" + minuteBucket, Duration.ofMinutes(2));
        if (minuteCount > RATE_LIMIT_PER_MINUTE) {
            return "提问过于频繁，请稍后再试";
        }

        // TTL 随每次获取刷新：请求最长 60s，180s 兜底自愈崩溃泄漏
        long running = state.increment(inflightKey(userId), Duration.ofSeconds(180));
        if (running > MAX_CONCURRENT_PER_USER) {
            state.decrement(inflightKey(userId));
            return "上一个问题还在处理中，请稍候";
        }

        state.increment(dailyKey(userId, today), Duration.ofHours(48));
        return null;
    }

    /** 释放并发在途计数（无论成功失败都要调用） */
    public void release(Integer userId) {
        state.decrement(inflightKey(userId));
    }
}