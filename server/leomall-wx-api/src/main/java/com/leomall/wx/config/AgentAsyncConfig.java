package com.leomall.wx.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阶段2C（审计 P1-8 异步化）：导购聊天专用有界线程池。
 *
 * chat 的阻塞部分（Agent HTTP 调用最长 45s + 主库校验/落库）全部移入本池，
 * Controller 返回 DeferredResult，Tomcat 工作线程即时释放——慢 chat 不再
 * 占用 Servlet 线程，普通接口（下单/首页）不受导购流量影响。
 *
 * 有界队列 + AbortPolicy：池满且队列满时立即拒绝（Controller 转 802 快速失败），
 * 而不是无界堆积拖垮内存。与既有多层闸门叠加：
 * 每用户并发 2 / 每分钟 6 / 日 100（AgentChatGuard）→ 本池 16+64 → AgentGateway 在途 20。
 */
@Configuration
public class AgentAsyncConfig {

    @Bean(name = "agentChatExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor agentChatExecutor(
            @Value("${leomall.agent.chat.threads:16}") int threads,
            @Value("${leomall.agent.chat.queue-capacity:64}") int queueCapacity) {
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger seq = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "agent-chat-" + seq.incrementAndGet());
                t.setDaemon(true);
                return t;
            }
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threads, threads,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                factory,
                new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}