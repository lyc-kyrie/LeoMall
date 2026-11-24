package com.leomall.wx.config;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.leomall.wx.util.JwtHelper;

/**
 * 启动时加载用户端 JWT 密钥（P0-1 修复）。
 *
 * 密钥来源：配置 leomall.jwt-secret，回退环境变量 LEOMALL_JWT_SECRET。
 * 未配置或长度不足 32 字符时抛异常使应用启动失败（fail-fast），
 * 杜绝历史上「密钥硬编码且等于请求头名」导致的任意用户 token 伪造。
 * 更换密钥即令所有存量 token 失效。
 */
@Component
public class JwtSecretInitializer {

    private static final Log logger = LogFactory.getLog(JwtSecretInitializer.class);

    @Value("${leomall.jwt-secret:${LEOMALL_JWT_SECRET:}}")
    private String jwtSecret;

    @PostConstruct
    public void init() {
        JwtHelper.configure(jwtSecret);
        logger.info("用户端 JWT 密钥已从配置加载（存量 token 全部失效）");
    }
}