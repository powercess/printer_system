package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cups")
public record CupsProperties(
    String host,
    int port,
    String username,
    String password,
    boolean secure
) {
    public CupsProperties {
        host = host != null ? host : "localhost";
        port = port > 0 ? port : 631;
        secure = secure; // 是否使用 HTTPS
    }

    public String getIppUrl() {
        String protocol = secure ? "https" : "http";
        return protocol + "://" + host + ":" + port;
    }

    public boolean hasAuth() {
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }
}