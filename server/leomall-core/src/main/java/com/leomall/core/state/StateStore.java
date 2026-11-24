package com.leomall.core.state;

import java.time.Duration;

/**
 * 阶段1：跨实例共享状态抽象。
 *
 * 两种实现按 leomall.redis.enabled 条件装配：
 * - RedisStateStore：生产/多实例，状态存 Redis（原子计数、SETNX、Lua 释放锁）；
 * - InMemoryStateStore：默认降级，本机开发无需 Redis，行为与单实例内存态一致。
 *
 * 所有 key 统一前缀 leomall:，TTL 由调用方显式给定，禁止无过期时间的键。
 */
public interface StateStore {

    String get(String key);

    void set(String key, String value, Duration ttl);

    boolean delete(String key);

    /** SET NX + TTL：占位/去重/锁的基础原语 */
    boolean setIfAbsent(String key, String value, Duration ttl);

    /** 原子 +1 并（重新）设置 TTL，返回自增后的值。用于固定窗口计数/失败计数。 */
    long increment(String key, Duration ttl);

    /** 原子 -1；结果 <=0 时自动删除键（防负数泄漏）。用于并发在途计数。 */
    long decrement(String key);

    /** 分布式锁：成功返回释放凭证 token，失败返回 null。TTL 兜底防死锁。 */
    String tryLock(String key, Duration ttl);

    /** 仅当 token 匹配时释放锁（不会误删他人重新获取的锁） */
    void unlock(String key, String token);
}