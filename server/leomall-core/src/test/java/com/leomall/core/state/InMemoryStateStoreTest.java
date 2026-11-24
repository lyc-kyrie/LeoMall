package com.leomall.core.state;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;

/**
 * 阶段1回归：InMemoryStateStore 语义测试（与 RedisStateStore 对齐）。
 * Redis 实现的等价验证已在真 Redis 7 容器上通过（见 docs/AUDIT.md 第八节）。
 */
public class InMemoryStateStoreTest {

    @Test
    public void setGetDeleteWithTtl() throws InterruptedException {
        StateStore s = new InMemoryStateStore();
        s.set("k", "v", Duration.ofMillis(120));
        Assert.assertEquals("v", s.get("k"));
        Thread.sleep(180);
        Assert.assertNull("TTL 过期后必须读不到", s.get("k"));

        s.set("k2", "v2", Duration.ofSeconds(10));
        Assert.assertTrue(s.delete("k2"));
        Assert.assertNull(s.get("k2"));
    }

    @Test
    public void setIfAbsentIsExclusive() {
        StateStore s = new InMemoryStateStore();
        Assert.assertTrue(s.setIfAbsent("nx", "1", Duration.ofSeconds(10)));
        Assert.assertFalse(s.setIfAbsent("nx", "2", Duration.ofSeconds(10)));
        Assert.assertEquals("1", s.get("nx"));
    }

    @Test
    public void incrementDecrement() {
        StateStore s = new InMemoryStateStore();
        Assert.assertEquals(1, s.increment("c", Duration.ofSeconds(10)));
        Assert.assertEquals(2, s.increment("c", Duration.ofSeconds(10)));
        Assert.assertEquals(1, s.decrement("c"));
        Assert.assertEquals(0, s.decrement("c"));
        Assert.assertNull("计数归零必须删键", s.get("c"));
        Assert.assertEquals("不存在键 decrement 返回 0", 0, s.decrement("missing"));
    }

    @Test
    public void lockTokenSemantics() throws InterruptedException {
        StateStore s = new InMemoryStateStore();
        String token = s.tryLock("lk", Duration.ofSeconds(10));
        Assert.assertNotNull(token);
        Assert.assertNull("锁必须互斥", s.tryLock("lk", Duration.ofSeconds(10)));
        s.unlock("lk", "wrong-token");
        Assert.assertNotNull("错误 token 不得释放锁", s.get("lk"));
        s.unlock("lk", token);
        Assert.assertNull(s.get("lk"));

        String token2 = s.tryLock("lk", Duration.ofMillis(120));
        Assert.assertNotNull(token2);
        Thread.sleep(180);
        Assert.assertNotNull("锁 TTL 到期必须自动释放", s.tryLock("lk", Duration.ofSeconds(10)));
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
        Assert.assertEquals("8000", s.get("hot"));
    }
}