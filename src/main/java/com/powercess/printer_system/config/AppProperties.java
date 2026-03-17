package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    UploadProperties upload,
    CupsProperties cups,
    PaymentProperties payment,
    String baseUrl
) {
    public AppProperties {
        baseUrl = baseUrl != null ? baseUrl : "https://printer.powercess.com";
    }
}