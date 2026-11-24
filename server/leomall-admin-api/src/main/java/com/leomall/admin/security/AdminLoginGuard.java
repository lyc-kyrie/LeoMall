package com.leomall.admin.security;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.leomall.core.state.StateStore;

/**
 * P0-3：管理端登录失败锁定（防爆破）。
 *
 * 同一账号 15 分钟窗口内连续失败 5 次锁定 15 分钟；登录成功即清零。
 * 阶段1：StateStore 支撑——启用 Redis 后计数与锁定跨实例共享，
 * 多实例部署下爆破者无法通过轮询实例绕过锁定。
 * 阶段2B：随 Shiro→Spring Security 迁移至 security 包，逻辑不变。
 */
@Component
public class AdminLoginGuard {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(15);

    private final StateStore state;

    public AdminLoginGuard(StateStore state) {
        this.state = state;
    }

    private static String failKey(String username) {
        return "leomall:admin:login:fail:" + username;
    }

    private static String lockKey(String username) {
        return "leomall:admin:login:lock:" + username;
    }

    public boolean isLocked(String username) {
        return state.get(lockKey(username)) != null;
    }

    public void recordFailure(String username) {
        // TTL 随每次失败刷新：窗口从最后一次失败起算
        long failures = state.increment(failKey(username), WINDOW);
        if (failures >= MAX_FAILURES) {
            state.set(lockKey(username), "1", LOCK);
            state.delete(failKey(username));
        }
    }

    public void recordSuccess(String username) {
        state.delete(failKey(username));
        state.delete(lockKey(username));
    }
}