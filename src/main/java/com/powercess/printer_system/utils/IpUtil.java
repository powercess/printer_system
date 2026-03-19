package com.powercess.printer_system.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtil {

    private IpUtil() {
        // 工具类禁止实例化
    }

    /**
     * 获取客户端真实IP地址
     * 支持通过代理访问的情况
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个 IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}