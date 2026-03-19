package com.powercess.printer_system.cups.impl;

import com.powercess.printer_system.cups.*;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJobAttributes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模拟 CUPS 实现
 * 用于开发和测试环境
 */
@Slf4j
public class MockCupsOperations implements CupsOperations {

    private final CupsServerConfig config;
    private volatile ConnectionStatus status = ConnectionStatus.CONNECTED;
    private final Map<String, MockPrinter> printers = new ConcurrentHashMap<>();
    private final AtomicInteger jobIdGenerator = new AtomicInteger(1);
    private final Map<Integer, MockJob> jobs = new ConcurrentHashMap<>();

    public MockCupsOperations(CupsServerConfig config) {
        this.config = config;
        // 添加一些模拟打印机
        addMockPrinter("Mock-Printer-1", "模拟打印机1", "Office");
        addMockPrinter("Mock-Printer-2", "模拟打印机2", "Library");
        log.info("Mock CUPS initialized for {}", config.id());
    }

    private void addMockPrinter(String name, String description, String location) {
        printers.put(name, new MockPrinter(name, description, location));
    }

    @Override
    public String getServerId() {
        return config.id();
    }

    @Override
    public boolean testConnection() {
        return status == ConnectionStatus.CONNECTED;
    }

    @Override
    public List<CupsPrinter> getPrinters() throws CupsException {
        // 返回模拟的打印机列表（实际使用时需要适配）
        return new ArrayList<>();
    }

    @Override
    public CupsPrinter getPrinter(String printerName) throws CupsException {
        if (printers.containsKey(printerName)) {
            return null; // 简化实现
        }
        throw new CupsException(CupsException.ErrorCode.PRINTER_NOT_FOUND, "打印机不存在: " + printerName);
    }

    @Override
    public Map<String, Object> getPrinterInfo(CupsPrinter printer) {
        MockPrinter mock = printers.get(printer.getName());
        if (mock != null) {
            Map<String, Object> info = new HashMap<>();
            info.put("name", mock.name);
            info.put("description", mock.description);
            info.put("location", mock.location);
            info.put("state", "IDLE");
            info.put("serverId", config.id());
            return info;
        }
        return Map.of();
    }

    @Override
    public int print(CupsPrinter printer, InputStream inputStream, String jobName, PrintOptions options) throws CupsException {
        int jobId = jobIdGenerator.getAndIncrement();
        MockJob job = new MockJob(jobId, jobName, "mock-user", "PENDING");
        jobs.put(jobId, job);
        log.info("Mock print job submitted: jobId={}, jobName={}, copies={}, duplex={}",
            jobId, jobName, options.copies(), options.duplex());

        // 模拟打印完成
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                job.state = "COMPLETED";
                log.info("Mock print job completed: {}", jobId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        return jobId;
    }

    @Override
    public boolean cancelJob(CupsPrinter printer, Integer jobId) throws CupsException {
        MockJob job = jobs.get(jobId);
        if (job != null) {
            job.state = "CANCELLED";
            log.info("Mock job {} cancelled", jobId);
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelJob(Integer jobId) throws CupsException {
        return cancelJob(null, jobId);
    }

    @Override
    public List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws CupsException {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MockJob job : jobs.values()) {
            if ("completed".equals(whichJobs) && !"COMPLETED".equals(job.state)) continue;
            if ("not-completed".equals(whichJobs) && "COMPLETED".equals(job.state)) continue;

            Map<String, Object> jobMap = new HashMap<>();
            jobMap.put("id", job.id);
            jobMap.put("name", job.name);
            jobMap.put("user", job.user);
            jobMap.put("state", job.state);
            jobMap.put("serverId", config.id());
            result.add(jobMap);
        }
        return result;
    }

    @Override
    public PrintJobAttributes getJobAttributes(Integer jobId) throws CupsException {
        return null;
    }

    @Override
    public void close() {
        status = ConnectionStatus.DISCONNECTED;
        log.info("Mock CUPS closed for {}", config.id());
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return status;
    }

    private record MockPrinter(String name, String description, String location) {}

    private static class MockJob {
        final int id;
        final String name;
        final String user;
        volatile String state;

        MockJob(int id, String name, String user, String state) {
            this.id = id;
            this.name = name;
            this.user = user;
            this.state = state;
        }
    }
}