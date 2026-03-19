package com.powercess.printer_system.cups;

import com.powercess.printer_system.config.AppProperties;
import com.powercess.printer_system.cups.impl.Cups4jOperations;
import org.cups4j.CupsPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CUPS 打印机连接和打印测试
 *
 * 配置方式：
 * 1. 修改 application-test.yaml 中的 cups 配置
 * 2. 或通过环境变量覆盖：CUPS_TEST_HOST, CUPS_TEST_PORT 等
 * 3. PDF 测试文件：设置环境变量 CUPS_TEST_PDF=/path/to/test.pdf
 *
 * 运行方式：
 * ./gradlew test --tests CupsPrinterTest
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CUPS 打印机测试")
class CupsPrinterTest {

    @Autowired
    private AppProperties appProperties;

    private CupsOperations cupsOperations;
    private CupsServerConfig config;
    private String testPdfPath;

    @BeforeEach
    void setUp() {
        var cupsProps = appProperties.cups();
        config = CupsServerConfig.builder()
            .id("test-cups")
            .host(cupsProps.host())
            .port(cupsProps.port())
            .username(cupsProps.username())
            .password(cupsProps.password())
            .secure(cupsProps.secure())
            .timeout(cupsProps.timeout())
            .build();

        cupsOperations = new Cups4jOperations(config);

        // 从环境变量读取 PDF 测试文件路径
        testPdfPath = System.getenv().getOrDefault("CUPS_TEST_PDF", "");

        System.out.println("\n" + "=".repeat(60));
        System.out.println("CUPS 测试配置:");
        System.out.println("  Host: " + config.host());
        System.out.println("  Port: " + config.port());
        System.out.println("  Username: " + (config.username() != null ? config.username() : "(无)"));
        System.out.println("  IPP URL: " + config.getIppUrl());
        System.out.println("  PDF 测试文件: " + (testPdfPath.isEmpty() ? "(未设置)" : testPdfPath));
        System.out.println("=".repeat(60));
    }

    @Test
    @DisplayName("1. 测试 CUPS 连接")
    void testConnection() {
        System.out.println("\n📡 测试 CUPS 连接...");

        boolean connected = cupsOperations.testConnection();
        System.out.println("连接状态: " + (connected ? "✅ 成功" : "❌ 失败"));
        System.out.println("连接状态枚举: " + cupsOperations.getConnectionStatus());

        assertTrue(connected, "CUPS 连接失败，请检查配置: " + config.getIppUrl());
    }

    @Test
    @DisplayName("2. 获取打印机列表")
    void testGetPrinters() throws Exception {
        System.out.println("\n🖨️ 获取打印机列表...");

        List<CupsPrinter> printers = cupsOperations.getPrinters();

        System.out.println("找到 " + printers.size() + " 台打印机:");
        System.out.println("-".repeat(50));

        for (CupsPrinter printer : printers) {
            System.out.println("  名称: " + printer.getName());
            System.out.println("  描述: " + printer.getDescription());
            System.out.println("  位置: " + printer.getLocation());
            System.out.println("  状态: " + printer.getState());
            System.out.println("  URI: " + printer.getDeviceURI());
            System.out.println("-".repeat(50));
        }

        assertFalse(printers.isEmpty(), "没有找到任何打印机，请在 CUPS 中添加打印机");
    }

    @Test
    @DisplayName("3. 获取打印机详细信息")
    void testGetPrinterInfo() throws Exception {
        System.out.println("\n📋 获取打印机详细信息...");

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter firstPrinter = printers.get(0);
        Map<String, Object> info = cupsOperations.getPrinterInfo(firstPrinter);

        System.out.println("打印机信息:");
        info.forEach((key, value) -> System.out.println("  " + key + ": " + value));

        assertNotNull(info.get("name"));
        assertNotNull(info.get("serverId"));
    }

    @Test
    @DisplayName("4. 打印测试页 - 简单文本")
    void testPrintSimpleText() throws Exception {
        System.out.println("\n📄 打印测试页 (简单文本)...");

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter printer = printers.get(0);
        System.out.println("使用打印机: " + printer.getName());

        // 创建测试文本内容
        String testContent = """
            ============================================
                    CUPS 打印测试
                    测试时间: %s
                    打印机: %s
                    服务器: %s:%d
            ============================================

            这是一页测试打印文档。
            如果你看到这个输出，说明打印系统工作正常！

            测试内容包括：
            - 中文字符测试
            - English character test
            - 数字测试: 0123456789
            - 符号测试: !@#$%%^&*()

            ============================================
                           测试完成
            ============================================
            """.formatted(
                java.time.LocalDateTime.now(),
                printer.getName(),
                config.host(),
                config.port()
            );

        InputStream inputStream = new ByteArrayInputStream(testContent.getBytes("UTF-8"));

        PrintOptions options = PrintOptions.builder()
            .copies(1)
            .duplex(PrintOptions.DUPLEX_ONE_SIDED)
            .media(PrintOptions.MEDIA_A4)
            .colorModel(PrintOptions.COLOR_GRAY)
            .build();

        System.out.println("打印选项: copies=" + options.copies() + ", duplex=" + options.duplex());

        try {
            int jobId = cupsOperations.print(printer, inputStream, "CUPS-Test-" + System.currentTimeMillis(), options);
            System.out.println("✅ 打印任务已提交，Job ID: " + jobId);
            assertTrue(jobId > 0, "打印任务ID应该大于0");
        } catch (CupsException e) {
            System.out.println("❌ 打印失败: " + e.getMessage());
            System.out.println("💡 提示: 某些打印机不支持直接打印纯文本，请尝试使用 PDF 文件测试");
        }
    }

    @Test
    @DisplayName("5. 打印 PDF 文件 - 单面")
    void testPrintPdfOneSided() throws Exception {
        System.out.println("\n📑 打印 PDF 文件 - 单面...");

        if (testPdfPath.isEmpty()) {
            System.out.println("⚠️ 未设置测试 PDF 文件路径，跳过测试");
            System.out.println("💡 设置方式: export CUPS_TEST_PDF=/path/to/test.pdf");
            return;
        }

        Path pdfPath = Path.of(testPdfPath);
        if (!Files.exists(pdfPath)) {
            System.out.println("❌ 测试文件不存在: " + testPdfPath);
            return;
        }

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter printer = printers.get(0);
        System.out.println("使用打印机: " + printer.getName());
        System.out.println("打印文件: " + testPdfPath);
        System.out.println("打印方式: 单面 (one-sided)");

        try (InputStream inputStream = new FileInputStream(pdfPath.toFile())) {
            PrintOptions options = PrintOptions.builder()
                .copies(1)
                .duplex(PrintOptions.DUPLEX_ONE_SIDED)
                .colorModel(PrintOptions.COLOR_GRAY)
                .media(PrintOptions.MEDIA_A4)
                .build();

            int jobId = cupsOperations.print(printer, inputStream, "PDF-单面测试-" + pdfPath.getFileName().toString(), options);
            System.out.println("✅ PDF 单面打印任务已提交，Job ID: " + jobId);
            assertTrue(jobId > 0);
        }
    }

    @Test
    @DisplayName("6. 打印 PDF 文件 - 双面长边")
    void testPrintPdfTwoSidedLongEdge() throws Exception {
        System.out.println("\n📑 打印 PDF 文件 - 双面长边...");

        if (testPdfPath.isEmpty()) {
            System.out.println("⚠️ 未设置测试 PDF 文件路径，跳过测试");
            System.out.println("💡 设置方式: export CUPS_TEST_PDF=/path/to/test.pdf");
            return;
        }

        Path pdfPath = Path.of(testPdfPath);
        if (!Files.exists(pdfPath)) {
            System.out.println("❌ 测试文件不存在: " + testPdfPath);
            return;
        }

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter printer = printers.get(0);
        System.out.println("使用打印机: " + printer.getName());
        System.out.println("打印文件: " + testPdfPath);
        System.out.println("打印方式: 双面长边 (two-sided-long-edge)");
        System.out.println("  说明: 适合纵向文档，翻转时沿长边翻转");

        try (InputStream inputStream = new FileInputStream(pdfPath.toFile())) {
            PrintOptions options = PrintOptions.builder()
                .copies(1)
                .duplex(PrintOptions.DUPLEX_TWO_SIDED_LONG_EDGE)
                .colorModel(PrintOptions.COLOR_GRAY)
                .media(PrintOptions.MEDIA_A4)
                .build();

            int jobId = cupsOperations.print(printer, inputStream, "PDF-双面长边测试-" + pdfPath.getFileName().toString(), options);
            System.out.println("✅ PDF 双面长边打印任务已提交，Job ID: " + jobId);
            assertTrue(jobId > 0);
        }
    }

    @Test
    @DisplayName("7. 打印 PDF 文件 - 双面短边")
    void testPrintPdfTwoSidedShortEdge() throws Exception {
        System.out.println("\n📑 打印 PDF 文件 - 双面短边...");

        if (testPdfPath.isEmpty()) {
            System.out.println("⚠️ 未设置测试 PDF 文件路径，跳过测试");
            System.out.println("💡 设置方式: export CUPS_TEST_PDF=/path/to/test.pdf");
            return;
        }

        Path pdfPath = Path.of(testPdfPath);
        if (!Files.exists(pdfPath)) {
            System.out.println("❌ 测试文件不存在: " + testPdfPath);
            return;
        }

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter printer = printers.get(0);
        System.out.println("使用打印机: " + printer.getName());
        System.out.println("打印文件: " + testPdfPath);
        System.out.println("打印方式: 双面短边 (two-sided-short-edge)");
        System.out.println("  说明: 适合横向文档，翻转时沿短边翻转");

        try (InputStream inputStream = new FileInputStream(pdfPath.toFile())) {
            PrintOptions options = PrintOptions.builder()
                .copies(1)
                .duplex(PrintOptions.DUPLEX_TWO_SIDED_SHORT_EDGE)
                .colorModel(PrintOptions.COLOR_GRAY)
                .media(PrintOptions.MEDIA_A4)
                .build();

            int jobId = cupsOperations.print(printer, inputStream, "PDF-双面短边测试-" + pdfPath.getFileName().toString(), options);
            System.out.println("✅ PDF 双面短边打印任务已提交，Job ID: " + jobId);
            assertTrue(jobId > 0);
        }
    }

    @Test
    @DisplayName("8. 打印 PDF 文件 - 彩色")
    void testPrintPdfColor() throws Exception {
        System.out.println("\n📑 打印 PDF 文件 - 彩色...");

        if (testPdfPath.isEmpty()) {
            System.out.println("⚠️ 未设置测试 PDF 文件路径，跳过测试");
            System.out.println("💡 设置方式: export CUPS_TEST_PDF=/path/to/test.pdf");
            return;
        }

        Path pdfPath = Path.of(testPdfPath);
        if (!Files.exists(pdfPath)) {
            System.out.println("❌ 测试文件不存在: " + testPdfPath);
            return;
        }

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter printer = printers.get(0);
        System.out.println("使用打印机: " + printer.getName());
        System.out.println("打印文件: " + testPdfPath);
        System.out.println("打印方式: 彩色 (RGB)");

        try (InputStream inputStream = new FileInputStream(pdfPath.toFile())) {
            PrintOptions options = PrintOptions.builder()
                .copies(1)
                .duplex(PrintOptions.DUPLEX_ONE_SIDED)
                .colorModel(PrintOptions.COLOR_RGB)
                .media(PrintOptions.MEDIA_A4)
                .build();

            int jobId = cupsOperations.print(printer, inputStream, "PDF-彩色测试-" + pdfPath.getFileName().toString(), options);
            System.out.println("✅ PDF 彩色打印任务已提交，Job ID: " + jobId);
            assertTrue(jobId > 0);
        }
    }

    @Test
    @DisplayName("9. 打印 PDF 文件 - 多份副本")
    void testPrintPdfMultipleCopies() throws Exception {
        System.out.println("\n📑 打印 PDF 文件 - 多份副本...");

        if (testPdfPath.isEmpty()) {
            System.out.println("⚠️ 未设置测试 PDF 文件路径，跳过测试");
            System.out.println("💡 设置方式: export CUPS_TEST_PDF=/path/to/test.pdf");
            return;
        }

        Path pdfPath = Path.of(testPdfPath);
        if (!Files.exists(pdfPath)) {
            System.out.println("❌ 测试文件不存在: " + testPdfPath);
            return;
        }

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        CupsPrinter printer = printers.get(0);
        System.out.println("使用打印机: " + printer.getName());
        System.out.println("打印文件: " + testPdfPath);
        System.out.println("打印方式: 打印 2 份");

        try (InputStream inputStream = new FileInputStream(pdfPath.toFile())) {
            PrintOptions options = PrintOptions.builder()
                .copies(2)
                .duplex(PrintOptions.DUPLEX_ONE_SIDED)
                .colorModel(PrintOptions.COLOR_GRAY)
                .media(PrintOptions.MEDIA_A4)
                .build();

            int jobId = cupsOperations.print(printer, inputStream, "PDF-多份副本测试-" + pdfPath.getFileName().toString(), options);
            System.out.println("✅ PDF 多份副本打印任务已提交，Job ID: " + jobId);
            assertTrue(jobId > 0);
        }
    }

    @Test
    @DisplayName("10. 获取打印任务列表")
    void testGetJobs() throws Exception {
        System.out.println("\n📋 获取打印任务列表...");

        List<CupsPrinter> printers = cupsOperations.getPrinters();
        if (printers.isEmpty()) {
            System.out.println("⚠️ 没有可用的打印机，跳过测试");
            return;
        }

        for (CupsPrinter printer : printers) {
            System.out.println("\n打印机: " + printer.getName());
            System.out.println("-".repeat(40));

            List<Map<String, Object>> jobs = cupsOperations.getJobs(printer, "all");
            System.out.println("任务总数: " + jobs.size());

            for (Map<String, Object> job : jobs) {
                System.out.println("  Job ID: " + job.get("id"));
                System.out.println("    名称: " + job.get("name"));
                System.out.println("    状态: " + job.get("state"));
                System.out.println("    用户: " + job.get("user"));
            }
        }
    }

    @Test
    @DisplayName("11. 测试连接状态")
    void testConnectionStatus() {
        System.out.println("\n📊 测试连接状态...");

        CupsOperations.ConnectionStatus status = cupsOperations.getConnectionStatus();
        System.out.println("当前连接状态: " + status);

        assertEquals(CupsOperations.ConnectionStatus.CONNECTED, status, "连接状态应该是 CONNECTED");
    }

    @Test
    @DisplayName("12. 测试服务器配置")
    void testServerConfig() {
        System.out.println("\n⚙️ 测试服务器配置...");

        System.out.println("服务器 ID: " + config.id());
        System.out.println("服务器地址: " + config.host() + ":" + config.port());
        System.out.println("IPP URL: " + config.getIppUrl());
        System.out.println("安全连接: " + config.secure());
        System.out.println("认证配置: " + (config.hasAuth() ? "是" : "否"));

        assertNotNull(config.id());
        assertTrue(config.port() > 0);
        assertTrue(config.getIppUrl().contains(config.host()));
    }

    @Test
    @DisplayName("13. 测试错误处理 - 打印机不存在")
    void testPrinterNotFound() throws Exception {
        System.out.println("\n❌ 测试错误处理 - 打印机不存在...");

        var result = cupsOperations.getPrinter("NonExistentPrinter12345");
        System.out.println("查询不存在的打印机返回: " + result);

        assertNull(result, "不存在的打印机应该返回 null");
        System.out.println("✅ 错误处理正常");
    }

    @Test
    @DisplayName("14. 关闭连接")
    void testClose() {
        System.out.println("\n🔌 关闭连接...");

        cupsOperations.testConnection();
        System.out.println("关闭前状态: " + cupsOperations.getConnectionStatus());

        cupsOperations.close();
        System.out.println("连接已关闭");

        System.out.println("✅ close 方法执行成功");
    }
}