package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.cups")
public record CupsProperties(
    String host,
    int port
) {
    public CupsProperties {
        host = host != null ? host : "192.168.1.149";
        port = port > 0 ? port : 631;
    }
}