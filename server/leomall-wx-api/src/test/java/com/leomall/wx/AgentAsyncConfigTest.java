package com.leomall.wx;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.leomall.wx.config.AgentAsyncConfig;

/**
 * 阶段2C：导购聊天线程池语义回归——有界队列满时快速拒绝（不无界堆积）、
 * 命名线程、任务正常执行。
 */
public class AgentAsyncConfigTest {

    private final AgentAsyncConfig config = new AgentAsyncConfig();

    @Test
    public void rejectWhenPoolAndQueueSaturated() throws Exception {
        ThreadPoolExecutor executor = config.agentChatExecutor(1, 1);
        CountDownLatch block = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                try {
                    block.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            // 第二个任务进入容量为 1 的队列
            executor.execute(() -> { });
            // 池满 + 队列满 → 立即拒绝
            Assertions.assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> { }));
        } finally {
            block.countDown();
            executor.shutdown();
            Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void namedDaemonThreadRunsTask() throws Exception {
        ThreadPoolExecutor executor = config.agentChatExecutor(1, 4);
        try {
            Future<String> future = executor.submit(() -> Thread.currentThread().getName());
            String name = future.get(5, TimeUnit.SECONDS);
            Assertions.assertTrue(name.startsWith("agent-chat-"), "线程名应为 agent-chat-*，实际 " + name);
            Future<Boolean> daemon = executor.submit(() -> Thread.currentThread().isDaemon());
            Assertions.assertTrue(daemon.get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
            Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void queuedTaskEventuallyRuns() throws Exception {
        ThreadPoolExecutor executor = config.agentChatExecutor(1, 2);
        CountDownLatch block = new CountDownLatch(1);
        try {
            executor.execute(() -> {
                try {
                    block.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Future<Integer> queued = executor.submit(() -> 42);
            Assertions.assertFalse(queued.isDone());
            block.countDown();
            Assertions.assertEquals(42, queued.get(5, TimeUnit.SECONDS));
        } finally {
            block.countDown();
            executor.shutdown();
            Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}