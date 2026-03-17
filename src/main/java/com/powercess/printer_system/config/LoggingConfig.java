package com.powercess.printer_system.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 日志配置类，支持通过环境变量 LOG_MODE 快速切换日志模式
 *
 * 三种预设模式:
 * - dev: 开发模式，输出详细日志 (DEBUG)
 * - prod: 生产模式，输出标准日志 (INFO)
 * - quiet: 静默模式，仅输出错误日志 (WARN)
 *
 * 也可以通过单独的环境变量覆盖特定日志级别:
 * - LOG_LEVEL_APP: 应用日志级别
 * - LOG_LEVEL_WEB: Web 请求日志级别
 * - LOG_LEVEL_DB: 数据库日志级别
 */
@Configuration
public class LoggingConfig {

    private static final Logger log = LoggerFactory.getLogger(LoggingConfig.class);

    @Value("${LOG_MODE:dev}")
    private String logMode;

    @Value("${LOG_LEVEL_APP:}")
    private String logLevelApp;

    @Value("${LOG_LEVEL_WEB:}")
    private String logLevelWeb;

    @Value("${LOG_LEVEL_DB:}")
    private String logLevelDb;

    @Value("${MYBATIS_LOG_IMPL:org.apache.ibatis.logging.stdout.StdOutImpl}")
    private String mybatisLogImpl;

    @PostConstruct
    public void init() {
        // 如果未单独设置日志级别，则根据 LOG_MODE 设置默认值
        if (logLevelApp == null || logLevelApp.isEmpty()) {
            System.setProperty("LOG_LEVEL_APP", getDefaultLevel());
        }
        if (logLevelWeb == null || logLevelWeb.isEmpty()) {
            System.setProperty("LOG_LEVEL_WEB", getWebLevel());
        }
        if (logLevelDb == null || logLevelDb.isEmpty()) {
            System.setProperty("LOG_LEVEL_DB", getDbLevel());
        }

        // 根据 LOG_MODE 设置 MyBatis 日志实现
        if (!"org.apache.ibatis.logging.stdout.StdOutImpl".equals(mybatisLogImpl)) {
            System.setProperty("MYBATIS_LOG_IMPL", mybatisLogImpl);
        } else if ("prod".equalsIgnoreCase(logMode) || "quiet".equalsIgnoreCase(logMode)) {
            System.setProperty("MYBATIS_LOG_IMPL", "org.apache.ibatis.logging.nologging.NoLoggingImpl");
        }

        log.info("日志配置已加载 - LOG_MODE: {}, App: {}, Web: {}, DB: {}, MyBatis: {}",
            logMode,
            System.getProperty("LOG_LEVEL_APP", getDefaultLevel()),
            System.getProperty("LOG_LEVEL_WEB", getWebLevel()),
            System.getProperty("LOG_LEVEL_DB", getDbLevel()),
            System.getProperty("MYBATIS_LOG_IMPL", mybatisLogImpl));
    }

    private String getDefaultLevel() {
        return switch (logMode.toLowerCase()) {
            case "prod" -> "INFO";
            case "quiet" -> "WARN";
            default -> "DEBUG"; // dev mode
        };
    }

    private String getWebLevel() {
        return switch (logMode.toLowerCase()) {
            case "quiet" -> "ERROR";
            default -> "INFO";
        };
    }

    private String getDbLevel() {
        return switch (logMode.toLowerCase()) {
            case "dev" -> "DEBUG";
            case "prod" -> "INFO";
            case "quiet" -> "ERROR";
            default -> "INFO";
        };
    }
}