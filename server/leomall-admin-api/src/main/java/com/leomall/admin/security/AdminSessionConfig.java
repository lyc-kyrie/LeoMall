package com.leomall.admin.security;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * 阶段2B：管理端会话（替代 Shiro SessionManager / RedisSessionDAO）。
 *
 * - leomall.redis.enabled=true：Spring Session Redis（键 leomall:admin:session:*），
 *   多实例共享会话；principal（LeomallAdmin）与 SecurityContext 走 JDK 序列化；
 * - 未启用（默认）：MapSessionRepository 单实例内存会话；
 * - 会话超时 6h（与原 Shiro 配置一致），滚动过期。
 *
 * Boot 的 SessionAutoConfiguration 已在 application-core.yml 排除，
 * 由 @EnableSpringHttpSession 显式装配，避免 store-type 自动探测干扰。
 */
@Configuration
@EnableSpringHttpSession
public class AdminSessionConfig {

    private static final Duration TIMEOUT = Duration.ofHours(6);

    @Bean
    @ConditionalOnProperty(prefix = "leomall.redis", name = "enabled", havingValue = "true")
    public SessionRepository<? extends Session> redisSessionRepository(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JdkSerializationRedisSerializer());
        template.setHashValueSerializer(new JdkSerializationRedisSerializer());
        template.afterPropertiesSet();

        RedisSessionRepository repository = new RedisSessionRepository(template);
        repository.setRedisKeyNamespace("leomall:admin:session");
        repository.setDefaultMaxInactiveInterval(TIMEOUT);
        return repository;
    }

    @Bean
    @ConditionalOnProperty(prefix = "leomall.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
    public SessionRepository<MapSession> mapSessionRepository() {
        MapSessionRepository repository = new MapSessionRepository(new java.util.concurrent.ConcurrentHashMap<>());
        repository.setDefaultMaxInactiveInterval(TIMEOUT);
        return repository;
    }

    /** P1-1 语义保持：JSESSIONID + HttpOnly + SameSite=Lax */
    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        return serializer;
    }
}