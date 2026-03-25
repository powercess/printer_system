package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    UploadProperties upload,
    CupsProperties cups,
    PaymentProperties payment,
    StorageProperties storage,
    String baseUrl,
    String frontendUrl
) {
    public AppProperties {
        baseUrl = baseUrl != null ? baseUrl : "https://printer.powercess.com";
        frontendUrl = frontendUrl != null ? frontendUrl : "http://localhost:3000";
    }
}