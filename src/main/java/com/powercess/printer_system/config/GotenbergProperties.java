package com.powercess.printer_system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gotenberg 服务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "gotenberg")
public class GotenbergProperties {

    /**
     * Gotenberg 服务基础 URL
     */
    private String url = "http://localhost:3000";

    /**
     * 转换超时时间（毫秒）
     */
    private int timeout = 60000;
}