package com.powercess.printer_system.config;

import com.powercess.printer_system.cups.CupsServerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * CUPS 配置属性
 * 支持单服务器和多服务器配置
 */
@ConfigurationProperties(prefix = "app.cups")
public record CupsProperties(
    // 单服务器配置（向后兼容）
    String host,
    int port,
    String username,
    String password,
    boolean secure,

    // 多服务器配置
    List<ServerConfig> servers,

    // 负载均衡策略
    String strategy,

    // 模拟模式（用于测试）
    boolean mockMode,

    // 连接超时（毫秒）
    int timeout
) {
    public CupsProperties {
        host = host != null ? host : "localhost";
        port = port > 0 ? port : 631;
        servers = servers != null ? servers : new ArrayList<>();
        strategy = strategy != null ? strategy : "priority";
        timeout = timeout > 0 ? timeout : 30000;
    }

    /**
     * 获取所有服务器配置列表
     * 如果没有配置多服务器，则使用单服务器配置
     */
    public List<CupsServerConfig> getServerConfigs() {
        List<CupsServerConfig> configs = new ArrayList<>();

        // 添加多服务器配置
        if (servers != null && !servers.isEmpty()) {
            for (int i = 0; i < servers.size(); i++) {
                ServerConfig sc = servers.get(i);
                configs.add(CupsServerConfig.builder()
                    .id(sc.id != null ? sc.id : "cups-" + i)
                    .host(sc.host)
                    .port(sc.port)
                    .username(sc.username)
                    .password(sc.password)
                    .secure(sc.secure)
                    .timeout(timeout)
                    .priority(sc.priority)
                    .build());
            }
        }

        // 如果没有多服务器配置，使用单服务器配置
        if (configs.isEmpty()) {
            configs.add(CupsServerConfig.builder()
                .id("default")
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .secure(secure)
                .timeout(timeout)
                .priority(0)
                .build());
        }

        return configs;
    }

    /**
     * 单服务器配置
     */
    public record ServerConfig(
        String id,
        String host,
        int port,
        String username,
        String password,
        boolean secure,
        int priority
    ) {
        public ServerConfig {
            host = host != null ? host : "localhost";
            port = port > 0 ? port : 631;
            priority = priority >= 0 ? priority : 0;
        }
    }
}