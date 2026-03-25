package com.powercess.printer_system.storage;

import com.powercess.printer_system.config.StorageProperties;
import com.powercess.printer_system.service.storage.S3StorageProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S3 存储测试
 *
 * 配置方式:
 * 1. 复制 .env.test.example 为 .env.test
 *    cp .env.test.example .env.test
 * 2. 编辑 .env.test，填入实际的 S3 配置
 * 3. 设置 STORAGE_TYPE=s3
 *
 * 运行方式:
 * ./gradlew test --tests S3StorageTest
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("S3 存储测试")
class S3StorageTest {

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${app.storage.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${app.storage.s3.region:}")
    private String s3Region;

    @Value("${app.storage.s3.bucket:}")
    private String s3Bucket;

    @Value("${app.storage.s3.access-key:}")
    private String s3AccessKey;

    @Value("${app.storage.s3.secret-key:}")
    private String s3SecretKey;

    @Value("${app.storage.s3.path-style:false}")
    private boolean s3PathStyle;

    @Value("${app.storage.s3.presigned-url-expiration:3600}")
    private int presignedUrlExpiration;

    @Autowired
    private StorageProperties storageProperties;

    private S3StorageProvider storageProvider;
    private String testKey;
    private boolean s3Configured;

    private boolean isS3Configured() {
        return "s3".equalsIgnoreCase(storageType) &&
               s3Endpoint != null && !s3Endpoint.isEmpty() &&
               s3Bucket != null && !s3Bucket.isEmpty() &&
               s3AccessKey != null && !s3AccessKey.isEmpty() &&
               s3SecretKey != null && !s3SecretKey.isEmpty();
    }

    @BeforeAll
    static void printHeader() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("S3 存储测试");
        System.out.println("=".repeat(60));
    }

    @BeforeEach
    void setUp() {
        System.out.println("\n" + "-".repeat(60));
        System.out.println("S3 存储测试配置:");
        System.out.println("  STORAGE_TYPE: " + storageType);
        System.out.println("  S3_ENDPOINT: " + (s3Endpoint == null || s3Endpoint.isEmpty() ? "(未设置)" : s3Endpoint));
        System.out.println("  S3_REGION: " + (s3Region == null || s3Region.isEmpty() ? "(未设置)" : s3Region));
        System.out.println("  S3_BUCKET: " + (s3Bucket == null || s3Bucket.isEmpty() ? "(未设置)" : s3Bucket));
        System.out.println("  S3_ACCESS_KEY: " + (s3AccessKey == null || s3AccessKey.isEmpty() ? "(未设置)" : "***已设置***"));
        System.out.println("  S3_SECRET_KEY: " + (s3SecretKey == null || s3SecretKey.isEmpty() ? "(未设置)" : "***已设置***"));
        System.out.println("  S3_PATH_STYLE: " + s3PathStyle);
        System.out.println("-".repeat(60));

        s3Configured = isS3Configured();

        if (!s3Configured) {
            System.out.println("\n⚠️ S3 未配置，测试将被跳过");
            System.out.println("配置方式:");
            System.out.println("  1. cp .env.test.example .env.test");
            System.out.println("  2. 编辑 .env.test 填入 S3 配置");
            System.out.println("  3. 设置 STORAGE_TYPE=s3");
            return;
        }

        StorageProperties.S3 s3Config = new StorageProperties.S3(
            s3Endpoint,
            s3Region == null || s3Region.isEmpty() ? "us-east-1" : s3Region,
            s3Bucket,
            s3AccessKey,
            s3SecretKey,
            s3PathStyle,
            presignedUrlExpiration
        );

        storageProvider = new S3StorageProvider(s3Config);
        testKey = "test-" + UUID.randomUUID().toString();
        System.out.println("✅ S3 存储提供者初始化成功");
    }

    @AfterEach
    void tearDown() {
        if (storageProvider != null) {
            try {
                // 清理测试文件
                if (testKey != null && storageProvider.exists(testKey)) {
                    storageProvider.delete(testKey);
                }
            } catch (Exception e) {
                // 忽略清理错误
            }
            storageProvider.close();
        }
    }

    // ==================== 基础连接测试 ====================

    @Test
    @DisplayName("1. 初始化 S3 客户端")
    void testInitialization() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📡 测试 S3 客户端初始化...");
        assertNotNull(storageProvider, "S3 存储提供者应该成功初始化");
        System.out.println("✅ S3 客户端初始化成功");
    }

    @Test
    @DisplayName("2. 获取存储前缀")
    void testGetPrefix() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📋 测试存储前缀...");
        String prefix = storageProvider.getPrefix();
        System.out.println("存储前缀: " + prefix);
        assertEquals("s3/", prefix, "S3 存储前缀应该是 's3/'");
    }

    // ==================== 文件上传测试 ====================

    @Test
    @DisplayName("3. 上传文本文件")
    void testUploadTextFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📤 测试上传文本文件...");

        String content = "Hello, S3 Storage Test! 测试中文内容。时间: " + java.time.LocalDateTime.now();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(contentBytes);

        String result = storageProvider.upload(
            testKey,
            inputStream,
            contentBytes.length,
            "text/plain"
        );

        System.out.println("上传结果: " + result);
        System.out.println("测试 Key: " + testKey);
        assertTrue(result.startsWith("s3/"), "上传结果应该以 's3/' 开头");
        assertTrue(result.contains(testKey), "上传结果应该包含测试 key");
        System.out.println("✅ 文本文件上传成功");
    }

    @Test
    @DisplayName("4. 上传二进制文件")
    void testUploadBinaryFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📤 测试上传二进制文件...");

        // 生成随机二进制数据
        byte[] binaryData = new byte[1024];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) (i % 256);
        }

        InputStream inputStream = new ByteArrayInputStream(binaryData);
        String result = storageProvider.upload(
            testKey,
            inputStream,
            binaryData.length,
            "application/octet-stream"
        );

        System.out.println("上传结果: " + result);
        assertTrue(result.startsWith("s3/"), "上传结果应该以 's3/' 开头");
        System.out.println("✅ 二进制文件上传成功");
    }

    @Test
    @DisplayName("5. 上传带路径的文件")
    void testUploadWithPath() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📤 测试上传带路径的文件...");

        testKey = "test-dir/" + UUID.randomUUID() + "/test-file.txt";
        String content = "Nested file content";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(contentBytes);

        String result = storageProvider.upload(
            testKey,
            inputStream,
            contentBytes.length,
            "text/plain"
        );

        System.out.println("带路径的 Key: " + testKey);
        System.out.println("上传结果: " + result);
        assertTrue(result.contains("test-dir/"), "结果应该包含路径");
        System.out.println("✅ 带路径文件上传成功");
    }

    // ==================== 文件下载测试 ====================

    @Test
    @DisplayName("6. 下载文件为 InputStream")
    void testDownloadAsStream() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📥 测试下载文件 (InputStream)...");

        // 先上传文件
        String originalContent = "Test content for download: " + UUID.randomUUID();
        byte[] contentBytes = originalContent.getBytes(StandardCharsets.UTF_8);
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );

        // 下载文件
        try (InputStream downloadedStream = storageProvider.download(testKey)) {
            byte[] downloadedBytes = downloadedStream.readAllBytes();
            String downloadedContent = new String(downloadedBytes, StandardCharsets.UTF_8);

            System.out.println("原始内容: " + originalContent);
            System.out.println("下载内容: " + downloadedContent);
            assertEquals(originalContent, downloadedContent, "下载的内容应该与原始内容一致");
        }
        System.out.println("✅ InputStream 下载成功");
    }

    @Test
    @DisplayName("7. 下载文件为字节数组")
    void testDownloadAsBytes() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📥 测试下载文件 (字节数组)...");

        // 先上传文件
        String originalContent = "Binary test content: " + UUID.randomUUID();
        byte[] contentBytes = originalContent.getBytes(StandardCharsets.UTF_8);
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );

        // 下载文件
        byte[] downloadedBytes = storageProvider.downloadBytes(testKey);
        String downloadedContent = new String(downloadedBytes, StandardCharsets.UTF_8);

        System.out.println("原始长度: " + contentBytes.length);
        System.out.println("下载长度: " + downloadedBytes.length);
        assertArrayEquals(contentBytes, downloadedBytes, "下载的字节数组应该与原始内容一致");
        System.out.println("✅ 字节数组下载成功");
    }

    // ==================== 文件存在检查测试 ====================

    @Test
    @DisplayName("8. 检查存在的文件")
    void testExistsForExistingFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🔍 测试检查存在的文件...");

        // 先上传文件
        String content = "Test file for exists check";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );

        boolean exists = storageProvider.exists(testKey);
        System.out.println("文件 " + testKey + " 是否存在: " + exists);
        assertTrue(exists, "上传的文件应该存在");
        System.out.println("✅ 存在文件检查成功");
    }

    @Test
    @DisplayName("9. 检查不存在的文件")
    void testExistsForNonExistingFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🔍 测试检查不存在的文件...");

        String nonExistingKey = "non-existing-" + UUID.randomUUID();
        boolean exists = storageProvider.exists(nonExistingKey);
        System.out.println("文件 " + nonExistingKey + " 是否存在: " + exists);
        assertFalse(exists, "不存在的文件应该返回 false");
        System.out.println("✅ 不存在文件检查成功");
    }

    // ==================== 文件删除测试 ====================

    @Test
    @DisplayName("10. 删除文件")
    void testDeleteFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🗑️ 测试删除文件...");

        // 先上传文件
        String content = "Test file for deletion";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );

        System.out.println("文件上传成功: " + testKey);
        assertTrue(storageProvider.exists(testKey), "文件应该存在");

        // 删除文件
        storageProvider.delete(testKey);
        System.out.println("文件删除命令已发送: " + testKey);

        // 验证删除
        boolean existsAfterDelete = storageProvider.exists(testKey);
        System.out.println("删除后文件是否存在: " + existsAfterDelete);
        assertFalse(existsAfterDelete, "删除后文件不应该存在");
        System.out.println("✅ 文件删除成功");
    }

    // ==================== 预签名 URL 测试 ====================

    @Test
    @DisplayName("11. 生成预签名 URL")
    void testGeneratePresignedUrl() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🔗 测试生成预签名 URL...");

        // 先上传文件
        String content = "Test file for presigned URL";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );

        // 生成预签名 URL
        String presignedUrl = storageProvider.getPresignedUrl(testKey, Duration.ofMinutes(10));

        System.out.println("预签名 URL: " + presignedUrl);
        assertNotNull(presignedUrl, "预签名 URL 不应该为 null");
        assertTrue(presignedUrl.contains(testKey), "预签名 URL 应该包含文件 key");
        assertTrue(presignedUrl.contains("X-Amz") || presignedUrl.contains("Signature") || presignedUrl.contains("sig"),
                  "预签名 URL 应该包含签名参数");
        System.out.println("✅ 预签名 URL 生成成功");
    }

    @Test
    @DisplayName("12. 使用默认过期时间生成预签名 URL")
    void testGeneratePresignedUrlWithDefaultExpiration() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🔗 测试使用默认过期时间生成预签名 URL...");

        // 先上传文件
        String content = "Test file for default presigned URL";
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );

        // 使用默认过期时间
        String presignedUrl = storageProvider.getPresignedUrl(testKey, null);

        System.out.println("预签名 URL (默认过期): " + presignedUrl);
        assertNotNull(presignedUrl, "预签名 URL 不应该为 null");
        System.out.println("✅ 默认过期时间预签名 URL 生成成功");
    }

    // ==================== 完整流程测试 ====================

    @Test
    @DisplayName("13. 完整 CRUD 流程测试")
    void testFullCrudFlow() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🔄 测试完整 CRUD 流程...");

        String content = "Full CRUD test content: " + UUID.randomUUID();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        // 1. 上传
        System.out.println("\n1️⃣ 上传文件...");
        String uploadResult = storageProvider.upload(
            testKey,
            new ByteArrayInputStream(contentBytes),
            contentBytes.length,
            "text/plain"
        );
        System.out.println("   上传结果: " + uploadResult);
        assertNotNull(uploadResult);

        // 2. 检查存在
        System.out.println("\n2️⃣ 检查文件存在...");
        boolean exists = storageProvider.exists(testKey);
        System.out.println("   存在: " + exists);
        assertTrue(exists);

        // 3. 下载
        System.out.println("\n3️⃣ 下载文件...");
        byte[] downloaded = storageProvider.downloadBytes(testKey);
        System.out.println("   下载大小: " + downloaded.length + " bytes");
        assertArrayEquals(contentBytes, downloaded);

        // 4. 生成预签名 URL
        System.out.println("\n4️⃣ 生成预签名 URL...");
        String url = storageProvider.getPresignedUrl(testKey, Duration.ofMinutes(5));
        System.out.println("   URL 长度: " + url.length());
        assertNotNull(url);

        // 5. 删除
        System.out.println("\n5️⃣ 删除文件...");
        storageProvider.delete(testKey);
        boolean existsAfterDelete = storageProvider.exists(testKey);
        System.out.println("   删除后存在: " + existsAfterDelete);
        assertFalse(existsAfterDelete);

        System.out.println("\n✅ 完整 CRUD 流程测试成功");
    }

    @Test
    @DisplayName("14. 大文件上传下载测试")
    void testLargeFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n📦 测试大文件上传下载...");

        // 生成 1MB 的测试数据
        int sizeInBytes = 1024 * 1024; // 1MB
        byte[] largeData = new byte[sizeInBytes];
        for (int i = 0; i < sizeInBytes; i++) {
            largeData[i] = (byte) (i % 256);
        }

        System.out.println("生成测试数据: " + sizeInBytes + " bytes (1MB)");

        // 上传
        long uploadStart = System.currentTimeMillis();
        storageProvider.upload(
            testKey,
            new ByteArrayInputStream(largeData),
            largeData.length,
            "application/octet-stream"
        );
        long uploadTime = System.currentTimeMillis() - uploadStart;
        System.out.println("上传耗时: " + uploadTime + "ms");

        // 下载
        long downloadStart = System.currentTimeMillis();
        byte[] downloaded = storageProvider.downloadBytes(testKey);
        long downloadTime = System.currentTimeMillis() - downloadStart;
        System.out.println("下载耗时: " + downloadTime + "ms");
        System.out.println("下载大小: " + downloaded.length + " bytes");

        assertEquals(sizeInBytes, downloaded.length, "下载的文件大小应该一致");
        assertArrayEquals(largeData, downloaded, "下载的内容应该一致");
        System.out.println("✅ 大文件测试成功");
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("15. 下载不存在的文件")
    void testDownloadNonExistingFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n❌ 测试下载不存在的文件...");

        String nonExistingKey = "non-existing-" + UUID.randomUUID();
        System.out.println("尝试下载: " + nonExistingKey);

        assertThrows(Exception.class, () -> {
            storageProvider.download(nonExistingKey);
        }, "下载不存在的文件应该抛出异常");

        System.out.println("✅ 错误处理正常");
    }

    @Test
    @DisplayName("16. 删除不存在的文件")
    void testDeleteNonExistingFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n❌ 测试删除不存在的文件...");

        String nonExistingKey = "non-existing-" + UUID.randomUUID();
        System.out.println("尝试删除: " + nonExistingKey);

        // 删除不存在的文件不应该抛出异常
        assertDoesNotThrow(() -> {
            storageProvider.delete(nonExistingKey);
        }, "删除不存在的文件不应该抛出异常");

        System.out.println("✅ 删除不存在的文件处理正常");
    }

    @Test
    @DisplayName("17. 生成不存在文件的预签名 URL")
    void testPresignedUrlForNonExistingFile() {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n❌ 测试生成不存在文件的预签名 URL...");

        String nonExistingKey = "non-existing-" + UUID.randomUUID();
        System.out.println("尝试生成预签名 URL: " + nonExistingKey);

        // 即使文件不存在，预签名 URL 也应该能生成
        String url = storageProvider.getPresignedUrl(nonExistingKey, Duration.ofMinutes(5));

        // 某些 S3 实现可能会返回 null，某些可能会返回 URL
        System.out.println("预签名 URL 结果: " + (url != null ? "已生成" : "null"));

        System.out.println("✅ 预签名 URL 错误处理正常");
    }

    // ==================== 并发测试 ====================

    @Test
    @DisplayName("18. 并发上传测试")
    void testConcurrentUpload() throws InterruptedException {
        org.junit.jupiter.api.Assumptions.assumeTrue(s3Configured, "S3 未配置，跳过测试");
        System.out.println("\n🔀 测试并发上传...");

        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        String[] keys = new String[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            keys[index] = "concurrent-" + index + "-" + UUID.randomUUID();
            threads[i] = new Thread(() -> {
                try {
                    String content = "Concurrent test " + index;
                    byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
                    storageProvider.upload(
                        keys[index],
                        new ByteArrayInputStream(contentBytes),
                        contentBytes.length,
                        "text/plain"
                    );
                    results[index] = storageProvider.exists(keys[index]);
                } catch (Exception e) {
                    System.out.println("线程 " + index + " 失败: " + e.getMessage());
                    results[index] = false;
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join(30000);
        }

        // 验证所有上传
        int successCount = 0;
        for (int i = 0; i < threadCount; i++) {
            if (results[i]) {
                successCount++;
            }
            // 清理
            try {
                storageProvider.delete(keys[i]);
            } catch (Exception e) {
                // 忽略
            }
        }

        System.out.println("并发上传成功: " + successCount + "/" + threadCount);
        assertEquals(threadCount, successCount, "所有并发上传都应该成功");
        System.out.println("✅ 并发上传测试成功");
    }
}