package com.leomall.admin.shiro;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;

/**
 * 阶段1：Shiro 会话 Redis 存储。
 *
 * 管理端会话（含 kaptcha 验证码所依赖的 HttpSession 链路）存 Redis：
 * - 键 leomall:admin:session:{id}，值 JDK 序列化（SimpleSession 可序列化）；
 * - TTL = 会话超时（6 小时），每次访问 update 刷新，过期由 Redis 自动清理，
 *   无需 Shiro 定时校验扫描；
 * - 多实例共享会话：登录/验证码/后续请求落到不同实例也能命中同一会话。
 *
 * 仅在 leomall.redis.enabled=true 时装配（见 ShiroConfig）。
 */
public class RedisSessionDAO extends AbstractSessionDAO {

    private static final String KEY_PREFIX = "leomall:admin:session:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisSessionDAO(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        storeSession(sessionId, session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        if (sessionId == null) {
            return null;
        }
        return (Session) redisTemplate.opsForValue().get(KEY_PREFIX + sessionId);
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        if (session == null || session.getId() == null) {
            throw new UnknownSessionException("session or session id is null");
        }
        storeSession(session.getId(), session);
    }

    @Override
    public void delete(Session session) {
        if (session != null && session.getId() != null) {
            redisTemplate.delete(KEY_PREFIX + session.getId());
        }
    }

    @Override
    public Collection<Session> getActiveSessions() {
        // SCAN 渐进遍历，避免 KEYS 阻塞 Redis
        return redisTemplate.execute((RedisCallback<Collection<Session>>) connection -> {
            List<Session> sessions = new ArrayList<>();
            ScanOptions options = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(200).build();
            Cursor<byte[]> cursor = connection.scan(options);
            try {
                while (cursor.hasNext()) {
                    byte[] value = connection.get(cursor.next());
                    if (value != null) {
                        Object deserialized = redisTemplate.getValueSerializer().deserialize(value);
                        if (deserialized instanceof Session) {
                            sessions.add((Session) deserialized);
                        }
                    }
                }
            } finally {
                // 阶段2A：Spring Data Redis 2.7 的 Cursor.close() 不再抛受检 IOException
                cursor.close();
            }
            return sessions;
        });
    }

    private void storeSession(Serializable sessionId, Session session) {
        long timeoutMillis = session.getTimeout();
        if (timeoutMillis <= 0) {
            timeoutMillis = TimeUnit.HOURS.toMillis(6);
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + sessionId, session, timeoutMillis, TimeUnit.MILLISECONDS);
    }
}