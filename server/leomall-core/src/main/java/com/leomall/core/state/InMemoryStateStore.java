package com.leomall.core.state;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阶段1：默认内存降级实现（leomall.redis.enabled=false）。
 *
 * 行为与 Redis 实现对齐：TTL 惰性过期 + 机会性清扫、按 key 原子计数、
 * token 匹配释放锁。仅供本机开发/单实例使用；多实例部署必须启用 Redis。
 */
public class InMemoryStateStore implements StateStore {

    private static class Entry {
        volatile String value;
        volatile long expireAt;

        Entry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

    private void sweepIfLarge() {
        if (map.size() > 10000) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, Entry>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                if (it.next().getValue().expireAt <= now) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public String get(String key) {
        Entry e = map.get(key);
        if (e == null) {
            return null;
        }
        if (e.expireAt <= System.currentTimeMillis()) {
            map.remove(key, e);
            return null;
        }
        return e.value;
    }

    @Override
    public void set(String key, String value, Duration ttl) {
        map.put(key, new Entry(value, System.currentTimeMillis() + ttl.toMillis()));
        sweepIfLarge();
    }

    @Override
    public boolean delete(String key) {
        return map.remove(key) != null;
    }

    @Override
    public boolean setIfAbsent(String key, String value, Duration ttl) {
        boolean[] created = new boolean[1];
        long now = System.currentTimeMillis();
        map.compute(key, (k, existing) -> {
            if (existing != null && existing.expireAt > now) {
                created[0] = false;
                return existing;
            }
            created[0] = true;
            return new Entry(value, now + ttl.toMillis());
        });
        sweepIfLarge();
        return created[0];
    }

    @Override
    public long increment(String key, Duration ttl) {
        long[] result = new long[1];
        long now = System.currentTimeMillis();
        map.compute(key, (k, existing) -> {
            Entry e = (existing == null || existing.expireAt <= now)
                    ? new Entry("0", now + ttl.toMillis()) : existing;
            long next = Long.parseLong(e.value) + 1;
            e.value = Long.toString(next);
            e.expireAt = now + ttl.toMillis();
            result[0] = next;
            return e;
        });
        sweepIfLarge();
        return result[0];
    }

    @Override
    public long decrement(String key) {
        long[] result = new long[1];
        map.compute(key, (k, existing) -> {
            if (existing == null || existing.expireAt <= System.currentTimeMillis()) {
                result[0] = 0;
                return null;
            }
            long next = Long.parseLong(existing.value) - 1;
            if (next <= 0) {
                result[0] = 0;
                return null;
            }
            existing.value = Long.toString(next);
            result[0] = next;
            return existing;
        });
        return result[0];
    }

    @Override
    public String tryLock(String key, Duration ttl) {
        String token = UUID.randomUUID().toString();
        return setIfAbsent(key, token, ttl) ? token : null;
    }

    @Override
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        map.computeIfPresent(key, (k, e) -> token.equals(e.value) ? null : e);
    }
}