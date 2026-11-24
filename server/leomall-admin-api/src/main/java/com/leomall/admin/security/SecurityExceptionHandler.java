package com.leomall.admin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.leomall.core.util.ResponseUtil;

/**
 * 阶段2B：替代 ShiroExceptionHandler，保持原 JSON 语义。
 *
 * 方法级 @PreAuthorize 抛出的 AccessDeniedException（含 AuthorizationDeniedException）：
 * - 未认证/匿名 → 501 请登录（原 UnauthenticatedException 语义）；
 * - 已认证但权限不足 → 506 无操作权限（原 AuthorizationException 语义）。
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // 必须先于 GlobalExceptionHandler 的 Exception 兜底，否则越权被转成 502
public class SecurityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public Object accessDenied(AccessDeniedException e) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseUtil.unlogin();
        }
        logger.warn("admin access denied: {}", e.getMessage());
        return ResponseUtil.unauthz();
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object unauthenticated(AuthenticationException e) {
        return ResponseUtil.unlogin();
    }
}