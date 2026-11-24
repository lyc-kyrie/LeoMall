package com.leomall.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * P1-1：CORS 来源白名单化。
 *
 * 原实现 allowedOrigin("*") + allowCredentials(true)，任意站点可携带凭据跨域读取接口。
 * 现改为配置化白名单：leomall.cors.allowed-origins（逗号分隔，可用环境变量
 * LEOMALL_CORS_ALLOWED_ORIGINS 注入）。默认空 = 不开放任何跨域来源——
 * 管理端/H5 开发走 devServer 同源代理、生产走 nginx 同源，均无需 CORS。
 * 仅在白名单非空时才允许携带凭据；预检缓存从 30 天降为 10 分钟。
 */
@Configuration
public class CorsConfig {

    @Value("${leomall.cors.allowed-origins:}")
    private String allowedOrigins;

    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            for (String origin : allowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    corsConfiguration.addAllowedOrigin(trimmed);
                }
            }
            corsConfiguration.setAllowCredentials(true);
        }
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setMaxAge(600L);
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig());
        return new CorsFilter(source);
    }
}