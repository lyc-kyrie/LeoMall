package com.leomall.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * IP地址相关工具类
 *
 * P2-4：不再无条件信任 X-Forwarded-For 等可伪造头。
 * 仅当直连对端是可信代理（回环/内网地址，如 nginx 容器）时才读取
 * X-Real-IP / X-Forwarded-For；后端端口被直接访问时一律使用 TCP 对端地址，
 * 审计日志、登录 IP、支付 spbillCreateIp 不再可被客户端伪造。
 */
public class IpUtil {

    private static final Log logger = LogFactory.getLog(IpUtil.class);

    public static String getIpAddr(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null) {
            return "";
        }
        if (isTrustedProxy(remoteAddr)) {
            String realIp = request.getHeader("X-Real-IP");
            if (isValidAddress(realIp)) {
                return realIp.trim();
            }
            String xff = request.getHeader("X-Forwarded-For");
            if (isValidAddress(xff)) {
                String first = xff.split(",")[0].trim();
                if (isValidAddress(first)) {
                    return first;
                }
            }
        }
        return remoteAddr;
    }

    /** 直连对端是否为可信代理：回环、内网（10/172.16/192.168）、链路本地 */
    private static boolean isTrustedProxy(String addr) {
        try {
            InetAddress inet = InetAddress.getByName(addr);
            return inet.isLoopbackAddress() || inet.isSiteLocalAddress() || inet.isLinkLocalAddress();
        } catch (Exception e) {
            logger.warn("无法解析对端地址: " + addr);
            return false;
        }
    }

    private static boolean isValidAddress(String value) {
        return value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value.trim());
    }
}