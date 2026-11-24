package com.leomall.core.state;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 阶段1：Redis 实现。计数与释放锁用 Lua 保证原子性；
 * 所有键强制 TTL，避免 Redis 内存无限增长。
 */
public class RedisStateStore implements StateStore {

    /** INCR + PEXPIRE 原子化（避免 incr 后崩溃导致键永不过期） */
    private static final RedisScript<Long> INCR_SCRIPT = new DefaultRedisScript<>(
            "local v = redis.call('INCR', KEYS[1]) "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[1]) "
                    + "return v", Long.class);

    /** DECR，结果 <=0 时删键（自愈负数/泄漏）；键不存在返回 0（与内存实现一致） */
    private static final RedisScript<Long> DECR_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 0 then return 0 end "
                    + "local v = redis.call('DECR', KEYS[1]) "
                    + "if v <= 0 then redis.call('DEL', KEYS[1]) end "
                    + "return v", Long.class);

    /** 比较 token 后删除，防止误删他人持有的锁 */
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('DEL', KEYS[1]) "
                    + "else return 0 end", Long.class);

    private final StringRedisTemplate redis;

    public RedisStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redis.delete(key));
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
    }

    @Override
    public long increment(String key, Duration ttl) {
        Long value = redis.execute(INCR_SCRIPT, Collections.singletonList(key),
                String.valueOf(ttl.toMillis()));
        return value == null ? 0L : value;
    }

    @Override
    public long decrement(String key) {
        Long value = redis.execute(DECR_SCRIPT, Collections.singletonList(key));
        return value == null ? 0L : value;
    }

    @Override
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        return setIfAbsent(key, token, ttl) ? token : null;
    }

    @Override
    public void unlock(String key, String token) {
        if (token != null) {
            redis.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        }
    }
}