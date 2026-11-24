package com.leomall.wx.service;

import java.time.Duration;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.leomall.core.state.StateStore;

/**
 * 短信验证码管理（阶段1：StateStore 支撑，Redis 启用后跨实例共享）。
 *
 * - 验证码一次性使用，有效期 1 分钟（SETNX 天然实现同号 1 分钟发送间隔）；
 * - 连续校验失败 5 次作废（P1-4）；
 * - 发送频控：同号每天 ≤10 条、同 IP 每小时 ≤20 条（P1-4，固定窗口原子计数）。
 */
@Component
public class CaptchaCodeManager {

    private static final Duration CODE_TTL = Duration.ofMinutes(1);
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int PHONE_DAILY_LIMIT = 10;
    private static final int IP_HOURLY_LIMIT = 20;

    private final StateStore state;

    public CaptchaCodeManager(StateStore state) {
        this.state = state;
    }

    private static String codeKey(String phoneNumber) {
        return "leomall:captcha:code:" + phoneNumber;
    }

    private static String failKey(String phoneNumber) {
        return "leomall:captcha:fail:" + phoneNumber;
    }

    /**
     * 添加到缓存。同一号码存在未过期验证码时返回 false（1 分钟发送间隔）。
     */
    public boolean addToCache(String phoneNumber, String code) {
        return state.setIfAbsent(codeKey(phoneNumber), code, CODE_TTL);
    }

    /** 获取缓存的验证码（过期由 StateStore TTL 处理） */
    public String getCachedCaptcha(String phoneNumber) {
        return state.get(codeKey(phoneNumber));
    }

    /**
     * P1-4：发送频控——同一号码每天上限 10 条、同一 IP 每小时上限 20 条。
     * 计数原子自增，超限请求同样计入（防滥用探测）。
     */
    public boolean allowSend(String phoneNumber, String ip) {
        long day = LocalDate.now().toEpochDay();
        long phoneCount = state.increment(
                "leomall:captcha:send:phone:" + phoneNumber + ":" + day, Duration.ofHours(24));
        if (phoneCount > PHONE_DAILY_LIMIT) {
            return false;
        }
        if (ip != null && !ip.isEmpty()) {
            long hourBucket = System.currentTimeMillis() / 3600_000L;
            long ipCount = state.increment(
                    "leomall:captcha:send:ip:" + ip + ":" + hourBucket, Duration.ofHours(1));
            if (ipCount > IP_HOURLY_LIMIT) {
                return false;
            }
        }
        return true;
    }

    /**
     * P1-4：统一校验入口。成功即销毁验证码（一次性使用）；
     * 连续失败 5 次作废当前验证码，防止 6 位数字码被在线爆破。
     */
    public boolean checkCaptcha(String phoneNumber, String code) {
        String cached = getCachedCaptcha(phoneNumber);
        if (cached == null || cached.isEmpty() || code == null) {
            return false;
        }
        if (cached.equals(code)) {
            invalidate(phoneNumber);
            return true;
        }
        long fails = state.increment(failKey(phoneNumber), CODE_TTL);
        if (fails >= MAX_VERIFY_ATTEMPTS) {
            invalidate(phoneNumber);
        }
        return false;
    }

    /** P1-4：销毁验证码及其失败计数 */
    public void invalidate(String phoneNumber) {
        state.delete(codeKey(phoneNumber));
        state.delete(failKey(phoneNumber));
    }
}