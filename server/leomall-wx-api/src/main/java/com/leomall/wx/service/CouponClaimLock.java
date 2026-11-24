package com.leomall.wx.service;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.leomall.core.state.StateStore;

/**
 * P1-3 / 阶段1：优惠券领取/兑换的「用户+券」串行锁。
 *
 * 配合 TransactionTemplate 在锁内完成「复查限领/总量 + 插入」，
 * 消除 check-then-act 竞态。StateStore 支撑：Redis 启用后为跨实例
 * 分布式锁（SETNX + token 释放 + TTL 兜底防死锁）；未启用时降级为
 * 进程内锁，行为与单实例一致。
 */
@Component
public class CouponClaimLock {

    /** 锁 TTL 兜底：持锁实例崩溃时 10 秒后自动释放，远大于正常事务耗时 */
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    /** 获取锁的最长等待时间（短自旋，避免请求线程长时间挂起） */
    private static final long WAIT_MILLIS = 3000L;

    private final StateStore state;

    public CouponClaimLock(StateStore state) {
        this.state = state;
    }

    /**
     * 尝试获取锁，最多等待 3 秒。
     *
     * @return 成功返回句柄（必须 close 释放）；超时返回 null
     */
    public Handle tryLock(Integer userId, Integer couponId) {
        String key = "leomall:lock:coupon:" + userId + ":" + couponId;
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        while (true) {
            String token = state.tryLock(key, LOCK_TTL);
            if (token != null) {
                return new Handle(key, token);
            }
            if (System.currentTimeMillis() >= deadline) {
                return null;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    /** 锁句柄：close 时按 token 释放，不会误删他人重新获取的锁 */
    public class Handle implements AutoCloseable {
        private final String key;
        private final String token;

        private Handle(String key, String token) {
            this.key = key;
            this.token = token;
        }

        @Override
        public void close() {
            state.unlock(key, token);
        }
    }
}