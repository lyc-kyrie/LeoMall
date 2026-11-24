package com.leomall.admin.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 阶段2B：Shiro → Spring Security 6。
 *
 * URL 规则与原 ShiroConfig 过滤链对齐：
 * - 登录/验证码/401/403/index 演示端点放行（index 的方法级注解仍生效）；
 * - 其余 /leomall/admin/** 需认证；
 * - wx 前台（/leomall/wx/**）及其余路径放行，wx 鉴权仍由 JwtHelper 在控制器内完成。
 *
 * 未认证/越权不返回 HTTP 401/403，而是重定向到 auth/401、auth/403 端点，
 * 由其输出原有 JSON 语义（501 请登录 / 506 无操作权限），前端拦截器零改动。
 *
 * CSRF 关闭：会话 Cookie 已 SameSite=Lax + HttpOnly，跨站 POST 无法携带 Cookie；
 * CORS 白名单默认空（P1-1）；且全部写接口为 JSON body（浏览器跨站表单无法构造）。
 * 与原 Shiro 时代一致（Shiro 亦无 CSRF 防护）。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/leomall/admin/auth/kaptcha",
                        "/leomall/admin/auth/login",
                        "/leomall/admin/auth/401",
                        "/leomall/admin/auth/403",
                        "/leomall/admin/index/**").permitAll()
                .requestMatchers("/leomall/admin/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                        response.sendRedirect("/leomall/admin/auth/401"))
                .accessDeniedHandler((request, response, e) ->
                        response.sendRedirect("/leomall/admin/auth/403")))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }

    @Bean
    public AuthenticationManager adminAuthenticationManager(AdminAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }
}