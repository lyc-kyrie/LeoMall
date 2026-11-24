package com.leomall.core.state;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 阶段1回归：InMemoryStateStore 语义测试（与 RedisStateStore 对齐）。
 * Redis 实现的等价验证已在真 Redis 7 容器上通过（见 docs/AUDIT.md 第八节）。
 */
public class InMemoryStateStoreTest {

    @Test
    public void setGetDeleteWithTtl() throws InterruptedException {
        StateStore s = new InMemoryStateStore();
        s.set("k", "v", Duration.ofMillis(120));
        Assertions.assertEquals("v", s.get("k"));
        Thread.sleep(180);
        Assertions.assertNull(s.get("k"), "TTL 过期后必须读不到");

        s.set("k2", "v2", Duration.ofSeconds(10));
        Assertions.assertTrue(s.delete("k2"));
        Assertions.assertNull(s.get("k2"));
    }

    @Test
    public void setIfAbsentIsExclusive() {
        StateStore s = new InMemoryStateStore();
        Assertions.assertTrue(s.setIfAbsent("nx", "1", Duration.ofSeconds(10)));
        Assertions.assertFalse(s.setIfAbsent("nx", "2", Duration.ofSeconds(10)));
        Assertions.assertEquals("1", s.get("nx"));
    }

    @Test
    public void incrementDecrement() {
        StateStore s = new InMemoryStateStore();
        Assertions.assertEquals(1, s.increment("c", Duration.ofSeconds(10)));
        Assertions.assertEquals(2, s.increment("c", Duration.ofSeconds(10)));
        Assertions.assertEquals(1, s.decrement("c"));
        Assertions.assertEquals(0, s.decrement("c"));
        Assertions.assertNull(s.get("c"), "计数归零必须删键");
        Assertions.assertEquals(0, s.decrement("missing"), "不存在键 decrement 返回 0");
    }

    @Test
    public void lockTokenSemantics() throws InterruptedException {
        StateStore s = new InMemoryStateStore();
        String token = s.tryLock("lk", Duration.ofSeconds(10));
        Assertions.assertNotNull(token);
        Assertions.assertNull(s.tryLock("lk", Duration.ofSeconds(10)), "锁必须互斥");
        s.unlock("lk", "wrong-token");
        Assertions.assertNotNull(s.get("lk"), "错误 token 不得释放锁");
        s.unlock("lk", token);
        Assertions.assertNull(s.get("lk"));

        String token2 = s.tryLock("lk", Duration.ofMillis(120));
        Assertions.assertNotNull(token2);
        Thread.sleep(180);
        Assertions.assertNotNull(s.tryLock("lk", Duration.ofSeconds(10)), "锁 TTL 到期必须自动释放");
    }

    @Test
    public void concurrentIncrementIsExact() throws InterruptedException {
        StateStore s = new InMemoryStateStore();
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    s.increment("hot", Duration.ofSeconds(30));
                }
            });
            threads[i].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        Assertions.assertEquals("8000", s.get("hot"));
    }
}