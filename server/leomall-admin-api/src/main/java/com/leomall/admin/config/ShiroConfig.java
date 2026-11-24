package com.leomall.admin.config;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import com.leomall.admin.shiro.AdminAuthorizingRealm;
import com.leomall.admin.shiro.AdminWebSessionManager;
import com.leomall.admin.shiro.RedisSessionDAO;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class ShiroConfig {

    @Bean
    public Realm realm() {
        return new AdminAuthorizingRealm();
    }

    @Bean
    public ShiroFilterFactoryBean shiroFilterFactoryBean(SecurityManager securityManager) {
        ShiroFilterFactoryBean shiroFilterFactoryBean = new ShiroFilterFactoryBean();
        shiroFilterFactoryBean.setSecurityManager(securityManager);
        Map<String, String> filterChainDefinitionMap = new LinkedHashMap<String, String>();
        filterChainDefinitionMap.put("/leomall/admin/auth/kaptcha", "anon");
        filterChainDefinitionMap.put("/leomall/admin/auth/login", "anon");
        filterChainDefinitionMap.put("/leomall/admin/auth/401", "anon");
        filterChainDefinitionMap.put("/leomall/admin/auth/index", "anon");
        filterChainDefinitionMap.put("/leomall/admin/auth/403", "anon");
        filterChainDefinitionMap.put("/leomall/admin/index/*", "anon");

        filterChainDefinitionMap.put("/leomall/admin/**", "authc");
        shiroFilterFactoryBean.setLoginUrl("/leomall/admin/auth/401");
        shiroFilterFactoryBean.setSuccessUrl("/leomall/admin/auth/index");
        shiroFilterFactoryBean.setUnauthorizedUrl("/leomall/admin/auth/403");
        shiroFilterFactoryBean.setFilterChainDefinitionMap(filterChainDefinitionMap);
        return shiroFilterFactoryBean;
    }

    /**
     * 阶段1：leomall.redis.enabled=true 时会话存 Redis（跨实例共享，
     * kaptcha 与登录请求落到不同实例也能命中同一会话）；否则保持内存会话。
     */
    @Bean
    @ConditionalOnProperty(name = "leomall.redis.enabled", havingValue = "true")
    public RedisSessionDAO redisSessionDAO(
            @Qualifier("shiroSessionRedisTemplate") RedisTemplate<String, Object> shiroSessionRedisTemplate) {
        return new RedisSessionDAO(shiroSessionRedisTemplate);
    }

    @Bean
    public SessionManager sessionManager(ObjectProvider<RedisSessionDAO> redisSessionDAOProvider) {
        AdminWebSessionManager sessionManager = new AdminWebSessionManager();
        RedisSessionDAO redisSessionDAO = redisSessionDAOProvider.getIfAvailable();
        if (redisSessionDAO != null) {
            sessionManager.setSessionDAO(redisSessionDAO);
            // 过期由 Redis TTL 处理，关闭 Shiro 定时校验扫描
            sessionManager.setSessionValidationSchedulerEnabled(false);
        }
        return sessionManager;
    }

    @Bean
    public DefaultWebSecurityManager defaultWebSecurityManager(SessionManager sessionManager) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(realm());
        securityManager.setSessionManager(sessionManager);
        return securityManager;
    }

    @Bean
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager) {
        AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor =
                new AuthorizationAttributeSourceAdvisor();
        authorizationAttributeSourceAdvisor.setSecurityManager(securityManager);
        return authorizationAttributeSourceAdvisor;
    }

    @Bean
    @DependsOn("lifecycleBeanPostProcessor")
    public static DefaultAdvisorAutoProxyCreator defaultAdvisorAutoProxyCreator() {
        DefaultAdvisorAutoProxyCreator creator = new DefaultAdvisorAutoProxyCreator();
        creator.setProxyTargetClass(true);
        return creator;
    }
}
