package com.leomall.core.util;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** P2-4 回归：仅信任来自可信代理的 X-Real-IP / X-Forwarded-For */
public class IpUtilTest {

    private HttpServletRequest request(String remoteAddr, String realIp, String xff) {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        Mockito.when(req.getRemoteAddr()).thenReturn(remoteAddr);
        Mockito.when(req.getHeader("X-Real-IP")).thenReturn(realIp);
        Mockito.when(req.getHeader("X-Forwarded-For")).thenReturn(xff);
        return req;
    }

    @Test
    public void publicPeerCannotSpoofHeaders() {
        HttpServletRequest req = request("203.0.113.7", "1.2.3.4", "5.6.7.8");
        Assertions.assertEquals("203.0.113.7", IpUtil.getIpAddr(req), "直连公网对端必须用 TCP 地址");
    }

    @Test
    public void loopbackProxyRealIpTrusted() {
        HttpServletRequest req = request("127.0.0.1", "198.51.100.9", null);
        Assertions.assertEquals("198.51.100.9", IpUtil.getIpAddr(req));
    }

    @Test
    public void siteLocalProxyXffFirstHop() {
        HttpServletRequest req = request("172.18.0.1", null, "198.51.100.9, 10.0.0.1");
        Assertions.assertEquals("198.51.100.9", IpUtil.getIpAddr(req));
    }

    @Test
    public void unknownHeaderFallsBack() {
        HttpServletRequest req = request("192.168.1.1", "unknown", "unknown");
        Assertions.assertEquals("192.168.1.1", IpUtil.getIpAddr(req));
    }

    @Test
    public void nullRemoteAddrReturnsEmpty() {
        HttpServletRequest req = request(null, "1.2.3.4", null);
        Assertions.assertEquals("", IpUtil.getIpAddr(req));
    }
}