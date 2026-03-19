package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.config.CupsProperties;
import com.powercess.printer_system.cups.*;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.service.CupsClientService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJobAttributes;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CUPS 客户端服务实现
 * 使用新的抽象层支持多服务器和故障转移
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CupsClientServiceImpl implements CupsClientService {

    private final CupsProperties cupsProperties;
    private CupsConnectionManager connectionManager;

    @PostConstruct
    public void init() {
        try {
            CupsConnectionManager.SelectionStrategy strategy =
                CupsConnectionManager.SelectionStrategy.valueOf(cupsProperties.strategy().toUpperCase());

            connectionManager = new CupsConnectionManager(
                cupsProperties.getServerConfigs(),
                strategy,
                cupsProperties.mockMode()
            );

            log.info("CUPS Connection Manager initialized with {} servers, strategy: {}, mock: {}",
                cupsProperties.getServerConfigs().size(),
                strategy,
                cupsProperties.mockMode());
        } catch (Exception e) {
            log.error("Failed to initialize CUPS Connection Manager: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (connectionManager != null) {
            connectionManager.close();
        }
    }

    private void ensureReady() {
        if (connectionManager == null) {
            throw new BusinessException(503, "CUPS服务不可用");
        }
    }

    @Override
    public boolean testConnection() {
        try {
            ensureReady();
            return connectionManager.testConnection();
        } catch (Exception e) {
            log.error("CUPS connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取所有服务器状态
     */
    public Map<String, CupsOperations.ConnectionStatus> getAllServerStatus() {
        ensureReady();
        return connectionManager.getAllServerStatus();
    }

    @Override
    public List<CupsPrinter> getPrinters() throws Exception {
        ensureReady();
        try {
            return connectionManager.getPrinters();
        } catch (CupsException e) {
            throw new BusinessException(503, "获取打印机列表失败: " + e.getMessage());
        }
    }

    @Override
    public CupsPrinter getPrinter(String printerName) throws Exception {
        ensureReady();
        try {
            return connectionManager.getPrinter(printerName);
        } catch (CupsException e) {
            if (e.getErrorCode() == CupsException.ErrorCode.PRINTER_NOT_FOUND) {
                return null;
            }
            throw new BusinessException(503, "获取打印机失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPrinterInfo(CupsPrinter printer) {
        Map<String, Object> info = connectionManager.getPrinterInfo(printer);
        return info != null ? info : new HashMap<>();
    }

    @Override
    public int printFile(String printerName, File file, String jobName, int copies, String duplex) throws Exception {
        ensureReady();
        CupsPrinter printer = getPrinter(printerName);
        if (printer == null) {
            throw new BusinessException(404, "打印机不存在: " + printerName);
        }
        return printFile(printer, file, jobName, copies, duplex);
    }

    @Override
    public int printFile(CupsPrinter printer, File file, String jobName, int copies, String duplex) throws Exception {
        try (InputStream inputStream = new FileInputStream(file)) {
            return printStream(printer, inputStream, jobName, copies, duplex);
        }
    }

    @Override
    public int printBytes(String printerName, byte[] content, String jobName, int copies, String duplex) throws Exception {
        ensureReady();
        CupsPrinter printer = getPrinter(printerName);
        if (printer == null) {
            throw new BusinessException(404, "打印机不存在: " + printerName);
        }
        return printStream(printer, new ByteArrayInputStream(content), jobName, copies, duplex);
    }

    @Override
    public int printStream(CupsPrinter printer, InputStream inputStream, String jobName, int copies, String duplex) throws Exception {
        ensureReady();
        try {
            PrintOptions options = PrintOptions.builder()
                .copies(copies)
                .duplex(duplex)
                .build();

            return connectionManager.print(printer, inputStream, jobName, options);
        } catch (CupsException e) {
            throw new BusinessException(500, "打印失败: " + e.getMessage());
        }
    }

    @Override
    public int printWithOptions(CupsPrinter printer, InputStream inputStream, String jobName, Map<String, String> options) throws Exception {
        ensureReady();
        try {
            PrintOptions.Builder builder = PrintOptions.builder();

            if (options.containsKey("copies")) {
                builder.copies(Integer.parseInt(options.get("copies")));
            }
            if (options.containsKey("sides")) {
                builder.duplex(options.get("sides"));
            }
            if (options.containsKey("media")) {
                builder.media(options.get("media"));
            }
            if (options.containsKey("ColorModel")) {
                builder.colorModel(options.get("ColorModel"));
            }

            return connectionManager.print(printer, inputStream, jobName, builder.build());
        } catch (CupsException e) {
            throw new BusinessException(500, "打印失败: " + e.getMessage());
        }
    }

    @Override
    public boolean cancelJob(Integer jobId) {
        ensureReady();
        try {
            return connectionManager.cancelJob(jobId);
        } catch (CupsException e) {
            log.error("Failed to cancel job {}: {}", jobId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean cancelJob(CupsPrinter printer, Integer jobId) {
        ensureReady();
        try {
            return connectionManager.cancelJob(printer, jobId);
        } catch (CupsException e) {
            log.error("Failed to cancel job {}: {}", jobId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws Exception {
        ensureReady();
        try {
            return connectionManager.getJobs(printer, whichJobs);
        } catch (CupsException e) {
            throw new BusinessException(503, "获取打印任务失败: " + e.getMessage());
        }
    }

    @Override
    public PrintJobAttributes getJobAttributes(Integer jobId) throws Exception {
        ensureReady();
        try {
            return connectionManager.getJobAttributes(jobId);
        } catch (CupsException e) {
            throw new BusinessException(404, "打印任务不存在: " + jobId);
        }
    }
}