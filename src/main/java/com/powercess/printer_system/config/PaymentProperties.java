package com.powercess.printer_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
    String pid,
    String key,
    String apiUrl,
    String submitUrl,
    String queryUrl
) {
    public PaymentProperties {
        apiUrl = apiUrl != null ? apiUrl : "https://api.payqixiang.cn/mapi.php";
        submitUrl = submitUrl != null ? submitUrl : "https://api.payqixiang.cn/submit.php";
        queryUrl = queryUrl != null ? queryUrl : "https://api.payqixiang.cn/api.php";
    }
}