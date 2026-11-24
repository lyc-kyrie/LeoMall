package com.leomall.core.state;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 阶段1：共享状态装配。
 *
 * leomall.redis.enabled=true  → RedisStateStore（多实例共享）；
 * 缺省/false                  → InMemoryStateStore（本机开发零依赖）。
 *
 * Redis 连接参数走标准 spring.data.redis.*（见 application-core.yml 的环境变量占位）。
 * enabled=false 时 Lettuce 连接工厂虽被创建但不会发起连接。
 *
 * 阶段2B：原 shiroSessionRedisTemplate 随 Shiro 移除；管理端会话改由
 * admin-api 的 AdminSessionConfig（Spring Session）装配。
 */
@Configuration
public class StateStoreConfiguration {

    @Bean
    @ConditionalOnProperty(name = "leomall.redis.enabled", havingValue = "true")
    public StateStore redisStateStore(StringRedisTemplate stringRedisTemplate) {
        return new RedisStateStore(stringRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "leomall.redis.enabled", havingValue = "false", matchIfMissing = true)
    public StateStore inMemoryStateStore() {
        return new InMemoryStateStore();
    }
}