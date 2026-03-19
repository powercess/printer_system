package com.powercess.printer_system.cups.impl;

import com.powercess.printer_system.cups.CupsException;
import com.powercess.printer_system.cups.CupsOperations;
import com.powercess.printer_system.cups.CupsServerConfig;
import com.powercess.printer_system.cups.PrintOptions;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.*;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Cups4j 库的实现
 */
@Slf4j
public class Cups4jOperations implements CupsOperations {

    private final CupsServerConfig config;
    private final AtomicReference<CupsClient> cupsClientRef = new AtomicReference<>();
    private volatile ConnectionStatus status = ConnectionStatus.DISCONNECTED;
    private volatile long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL = 60000; // 1分钟

    public Cups4jOperations(CupsServerConfig config) {
        this.config = config;
        initialize();
    }

    private void initialize() {
        try {
            URL cupsUrl = URI.create(config.getIppUrl()).toURL();
            CupsClient client;

            if (config.hasAuth()) {
                client = new CupsClient(
                    cupsUrl.getHost(),
                    cupsUrl.getPort(),
                    config.username()
                );
            } else {
                client = new CupsClient(cupsUrl.getHost(), cupsUrl.getPort());
            }

            cupsClientRef.set(client);
            status = ConnectionStatus.CONNECTED;
            log.info("CUPS client initialized for server {}:{}", config.host(), config.port());
        } catch (Exception e) {
            status = ConnectionStatus.ERROR;
            log.error("Failed to initialize CUPS client for {}: {}", config.id(), e.getMessage());
        }
    }

    private void ensureClientReady() throws CupsException {
        CupsClient client = cupsClientRef.get();
        if (client == null) {
            // 尝试重新初始化
            initialize();
            client = cupsClientRef.get();
            if (client == null) {
                throw new CupsException(CupsException.ErrorCode.CONNECTION_FAILED,
                    "CUPS服务不可用: " + config.id());
            }
        }
    }

    @Override
    public String getServerId() {
        return config.id();
    }

    @Override
    public boolean testConnection() {
        try {
            ensureClientReady();
            getPrinters();
            status = ConnectionStatus.CONNECTED;
            lastHealthCheckTime = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            log.warn("CUPS connection test failed for {}: {}", config.id(), e.getMessage());
            status = ConnectionStatus.DISCONNECTED;
            return false;
        }
    }

    @Override
    public List<CupsPrinter> getPrinters() throws CupsException {
        ensureClientReady();
        try {
            return cupsClientRef.get().getPrinters();
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.CONNECTION_FAILED,
                "获取打印机列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public CupsPrinter getPrinter(String printerName) throws CupsException {
        ensureClientReady();
        try {
            return cupsClientRef.get().getPrinter(printerName);
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.PRINTER_NOT_FOUND,
                "获取打印机失败: " + printerName, e);
        }
    }

    @Override
    public Map<String, Object> getPrinterInfo(CupsPrinter printer) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", printer.getName());
        info.put("description", printer.getDescription());
        info.put("location", printer.getLocation());
        info.put("state", printer.getState());
        info.put("deviceUri", printer.getDeviceURI());
        info.put("printerUri", printer.getPrinterURL());
        info.put("serverId", config.id());
        return info;
    }

    @Override
    public int print(CupsPrinter printer, InputStream inputStream, String jobName, PrintOptions options) throws CupsException {
        ensureClientReady();
        try {
            PrintJob.Builder builder = new PrintJob.Builder(inputStream)
                .jobName(jobName)
                .copies(options.copies())
                .duplex(options.isDuplex())
                .pageFormat(options.media())
                .color(options.isColor());

            PrintRequestResult result = printer.print(builder.build());

            if (result.isSuccessfulResult()) {
                log.info("Print job submitted successfully to {}, jobId: {}", config.id(), result.getJobId());
                return result.getJobId();
            } else {
                throw new CupsException(CupsException.ErrorCode.PRINT_FAILED,
                    "打印任务提交失败: " + result.getResultMessage());
            }
        } catch (CupsException e) {
            throw e;
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.PRINT_FAILED,
                "打印失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelJob(CupsPrinter printer, Integer jobId) throws CupsException {
        ensureClientReady();
        try {
            boolean result = cupsClientRef.get().cancelJob(printer, jobId);
            if (result) {
                log.info("Job {} cancelled on {}", jobId, config.id());
            }
            return result;
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.CANCEL_FAILED,
                "取消打印任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelJob(Integer jobId) throws CupsException {
        ensureClientReady();
        try {
            List<CupsPrinter> printers = getPrinters();
            for (CupsPrinter printer : printers) {
                List<PrintJobAttributes> jobs = cupsClientRef.get().getJobs(printer, WhichJobsEnum.ALL, null, false);
                for (PrintJobAttributes job : jobs) {
                    if (job.getJobID() == jobId) {
                        return cancelJob(printer, jobId);
                    }
                }
            }
            log.warn("Job {} not found on {}", jobId, config.id());
            return false;
        } catch (CupsException e) {
            throw e;
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.CANCEL_FAILED,
                "取消打印任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws CupsException {
        ensureClientReady();
        try {
            WhichJobsEnum whichJobsEnum = switch (whichJobs) {
                case "completed" -> WhichJobsEnum.COMPLETED;
                case "all" -> WhichJobsEnum.ALL;
                default -> WhichJobsEnum.NOT_COMPLETED;
            };

            List<PrintJobAttributes> jobs = cupsClientRef.get().getJobs(printer, whichJobsEnum, null, false);

            List<Map<String, Object>> result = new ArrayList<>();
            for (PrintJobAttributes job : jobs) {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("id", job.getJobID());
                jobMap.put("name", job.getJobName());
                jobMap.put("user", job.getUserName());
                jobMap.put("state", job.getJobState() != null ? job.getJobState().name() : "UNKNOWN");
                jobMap.put("pagesPrinted", job.getPagesPrinted());
                jobMap.put("size", job.getSize());
                jobMap.put("createTime", job.getJobCreateTime());
                jobMap.put("completeTime", job.getJobCompleteTime());
                jobMap.put("jobUrl", job.getJobURL());
                jobMap.put("printerUrl", job.getPrinterURL());
                jobMap.put("serverId", config.id());
                result.add(jobMap);
            }
            return result;
        } catch (CupsException e) {
            throw e;
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.UNKNOWN, "获取打印任务失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PrintJobAttributes getJobAttributes(Integer jobId) throws CupsException {
        ensureClientReady();
        try {
            return cupsClientRef.get().getJobAttributes(jobId);
        } catch (Exception e) {
            throw new CupsException(CupsException.ErrorCode.JOB_NOT_FOUND,
                "获取打印任务属性失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        CupsClient client = cupsClientRef.getAndSet(null);
        if (client != null) {
            status = ConnectionStatus.DISCONNECTED;
            log.info("CUPS client closed for {}", config.id());
        }
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        // 定期健康检查
        long now = System.currentTimeMillis();
        if (now - lastHealthCheckTime > HEALTH_CHECK_INTERVAL) {
            testConnection();
        }
        return status;
    }

    /**
     * 重新连接
     */
    public void reconnect() {
        close();
        initialize();
    }

    public CupsServerConfig getConfig() {
        return config;
    }
}