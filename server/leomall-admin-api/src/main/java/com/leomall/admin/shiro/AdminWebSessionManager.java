package com.leomall.admin.shiro;

import com.alibaba.druid.util.StringUtils;
import org.apache.shiro.web.servlet.ShiroHttpServletRequest;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.Serializable;

public class AdminWebSessionManager extends DefaultWebSessionManager {

    public static final String LOGIN_TOKEN_KEY = "X-Leomall-Admin-Token";
    private static final String REFERENCED_SESSION_ID_SOURCE = "Stateless request";


    public AdminWebSessionManager() {
        super();
        setGlobalSessionTimeout(MILLIS_PER_HOUR * 6);
        // P1-1：会话 Cookie 加固。kaptcha 依赖 Cookie 维持会话，不能直接禁用；
        // 改为 HttpOnly + SameSite=Lax，阻断跨站脚本读取与跨站请求携带，
        // 配合 CORS 白名单消除跨域凭据滥用。业务请求仍走 X-Leomall-Admin-Token 头。
        SimpleCookie sessionCookie = new SimpleCookie("LEOMALLADMINSESSIONID");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/");
        sessionCookie.setSameSite(SimpleCookie.SameSiteOptions.LAX);
        setSessionIdCookie(sessionCookie);
        setSessionIdCookieEnabled(true);
        setSessionIdUrlRewritingEnabled(false);
    }

    @Override
    protected Serializable getSessionId(ServletRequest request, ServletResponse response) {
        String id = WebUtils.toHttp(request).getHeader(LOGIN_TOKEN_KEY);
        if (!StringUtils.isEmpty(id)) {
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_SOURCE, REFERENCED_SESSION_ID_SOURCE);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID, id);
            request.setAttribute(ShiroHttpServletRequest.REFERENCED_SESSION_ID_IS_VALID, Boolean.TRUE);
            // 参考 https://blog.csdn.net/narutots/article/details/120363843
            request.setAttribute(ShiroHttpServletRequest.SESSION_ID_URL_REWRITING_ENABLED, this.isSessionIdUrlRewritingEnabled());
            return id;
        } else {
            return super.getSessionId(request, response);
        }
    }
}
