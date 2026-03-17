package com.powercess.printer_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnvConfigTest {

    @Value("${DB_HOST:NOT_SET}")
    private String dbHost;

    @Value("${DB_PORT:NOT_SET}")
    private String dbPort;

    @Value("${DB_NAME:NOT_SET}")
    private String dbName;

    @Value("${DB_USERNAME:NOT_SET}")
    private String dbUsername;

    @Value("${DB_PASSWORD:NOT_SET}")
    private String dbPassword;

    @Value("${spring.datasource.url:NOT_SET}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:NOT_SET}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:NOT_SET}")
    private String datasourcePassword;

    @Test
    void testEnvVariablesLoaded() {
        System.out.println("========== 环境变量测试 ==========");
        System.out.println("DB_HOST: " + dbHost);
        System.out.println("DB_PORT: " + dbPort);
        System.out.println("DB_NAME: " + dbName);
        System.out.println("DB_USERNAME: " + dbUsername);
        System.out.println("DB_PASSWORD: " + (dbPassword.equals("NOT_SET") ? "NOT_SET" : "******"));

        System.out.println("\n========== Spring DataSource 配置 ==========");
        System.out.println("datasource.url: " + datasourceUrl);
        System.out.println("datasource.username: " + datasourceUsername);
        System.out.println("datasource.password: " + (datasourcePassword.equals("NOT_SET") ? "NOT_SET" : "******"));

        // 断言检查
        assertNotEquals("NOT_SET", dbHost, "DB_HOST 未设置");
        assertNotEquals("NOT_SET", datasourceUrl, "datasource.url 未设置");

        System.out.println("\n========== 测试结果 ==========");
        if (dbHost.equals("NOT_SET")) {
            System.out.println("❌ .env 文件未被正确加载！");
        } else {
            System.out.println("✅ .env 文件已正确加载！");
        }
    }
}