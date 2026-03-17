package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
    String dir
) {
    public UploadProperties {
        dir = dir != null ? dir : "./uploads";
    }
}