package com.powercess.printer_system.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * 时区配置
 * 统一设置应用时区为 Asia/Shanghai (东八区)
 */
@Configuration
public class TimeZoneConfig {

    @PostConstruct
    public void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        System.setProperty("user.timezone", "Asia/Shanghai");
    }
}