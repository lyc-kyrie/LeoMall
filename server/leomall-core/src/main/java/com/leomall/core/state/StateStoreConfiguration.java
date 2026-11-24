package com.leomall.core.state;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 阶段1：共享状态装配。
 *
 * leomall.redis.enabled=true  → RedisStateStore（多实例共享）+ Shiro 会话序列化模板；
 * 缺省/false                  → InMemoryStateStore（本机开发零依赖）。
 *
 * Redis 连接参数走标准 spring.redis.*（见 application-core.yml 的环境变量占位）。
 * enabled=false 时 Lettuce 连接工厂虽被创建但不会发起连接。
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

    /**
     * Shiro 会话专用模板：SimpleSession 及其属性均为 Serializable，
     * 用 JDK 序列化存值、字符串存键（leomall:admin:session:{id}）。
     */
    @Bean
    @ConditionalOnProperty(name = "leomall.redis.enabled", havingValue = "true")
    public RedisTemplate<String, Object> shiroSessionRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setHashValueSerializer(new JdkSerializationRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}