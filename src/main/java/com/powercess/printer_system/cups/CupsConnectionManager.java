package com.powercess.printer_system.cups;

import com.powercess.printer_system.cups.impl.Cups4jOperations;
import com.powercess.printer_system.cups.impl.MockCupsOperations;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.CupsPrinter;
import org.cups4j.PrintJobAttributes;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CUPS 连接管理器
 * 支持多服务器、健康检查、故障转移、负载均衡
 */
@Slf4j
public class CupsConnectionManager implements CupsOperations {

    private final Map<String, CupsOperations> connections = new ConcurrentHashMap<>();
    private final List<CupsServerConfig> serverConfigs;
    private final SelectionStrategy selectionStrategy;
    private final boolean mockMode;

    private final AtomicReference<String> activeServerId = new AtomicReference<>();

    public CupsConnectionManager(List<CupsServerConfig> configs, SelectionStrategy strategy, boolean mockMode) {
        this.serverConfigs = new ArrayList<>(configs);
        this.selectionStrategy = strategy != null ? strategy : SelectionStrategy.PRIORITY;
        this.mockMode = mockMode;

        initializeConnections();
    }

    private void initializeConnections() {
        for (CupsServerConfig config : serverConfigs) {
            try {
                CupsOperations operations = mockMode
                    ? new MockCupsOperations(config)
                    : new Cups4jOperations(config);
                connections.put(config.id(), operations);
                log.info("Initialized CUPS connection: {} ({}:{})", config.id(), config.host(), config.port());
            } catch (Exception e) {
                log.error("Failed to initialize CUPS connection {}: {}", config.id(), e.getMessage());
            }
        }

        // 设置默认活动服务器
        if (!connections.isEmpty()) {
            activeServerId.set(connections.keySet().iterator().next());
        }
    }

    /**
     * 获取一个可用的CUPS连接
     */
    public CupsOperations getAvailableConnection() {
        return getAvailableConnection(null);
    }

    /**
     * 获取一个可用的CUPS连接（优先使用指定的服务器）
     */
    public CupsOperations getAvailableConnection(String preferredServerId) {
        if (preferredServerId != null && connections.containsKey(preferredServerId)) {
            CupsOperations ops = connections.get(preferredServerId);
            if (ops.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                return ops;
            }
        }

        // 根据策略选择
        List<CupsOperations> available = connections.values().stream()
            .filter(ops -> ops.getConnectionStatus() == ConnectionStatus.CONNECTED)
            .toList();

        if (available.isEmpty()) {
            log.warn("No available CUPS connections");
            return connections.values().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No CUPS connections configured"));
        }

        return switch (selectionStrategy) {
            case PRIORITY -> selectByPriority(available);
            case ROUND_ROBIN -> selectByRoundRobin(available);
            case RANDOM -> selectByRandom(available);
        };
    }

    private CupsOperations selectByPriority(List<CupsOperations> available) {
        return available.stream()
            .max(Comparator.comparingInt(ops -> getConfig(ops).priority()))
            .orElse(available.get(0));
    }

    private int roundRobinIndex = 0;
    private CupsOperations selectByRoundRobin(List<CupsOperations> available) {
        return available.get(roundRobinIndex++ % available.size());
    }

    private CupsOperations selectByRandom(List<CupsOperations> available) {
        return available.get(new Random().nextInt(available.size()));
    }

    private CupsServerConfig getConfig(CupsOperations ops) {
        return serverConfigs.stream()
            .filter(c -> c.id().equals(ops.getServerId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * 定时健康检查
     */
    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        log.debug("Performing CUPS health check...");
        for (CupsOperations ops : connections.values()) {
            try {
                boolean healthy = ops.testConnection();
                log.debug("CUPS {} health: {}", ops.getServerId(), healthy ? "OK" : "FAILED");
            } catch (Exception e) {
                log.warn("Health check failed for {}: {}", ops.getServerId(), e.getMessage());
            }
        }
    }

    /**
     * 获取所有服务器状态
     */
    public Map<String, ConnectionStatus> getAllServerStatus() {
        Map<String, ConnectionStatus> status = new LinkedHashMap<>();
        connections.forEach((id, ops) -> status.put(id, ops.getConnectionStatus()));
        return status;
    }

    /**
     * 添加新服务器连接
     */
    public void addConnection(CupsServerConfig config) {
        if (connections.containsKey(config.id())) {
            log.warn("Connection {} already exists", config.id());
            return;
        }
        try {
            CupsOperations operations = mockMode
                ? new MockCupsOperations(config)
                : new Cups4jOperations(config);
            connections.put(config.id(), operations);
            log.info("Added CUPS connection: {}", config.id());
        } catch (Exception e) {
            log.error("Failed to add CUPS connection {}: {}", config.id(), e.getMessage());
        }
    }

    /**
     * 移除服务器连接
     */
    public void removeConnection(String serverId) {
        CupsOperations ops = connections.remove(serverId);
        if (ops != null) {
            ops.close();
            log.info("Removed CUPS connection: {}", serverId);
        }
    }

    // ==================== 实现 CupsOperations 接口 ====================

    @Override
    public String getServerId() {
        return getAvailableConnection().getServerId();
    }

    @Override
    public boolean testConnection() {
        return connections.values().stream().anyMatch(CupsOperations::testConnection);
    }

    @Override
    public List<CupsPrinter> getPrinters() throws CupsException {
        // 聚合所有服务器的打印机
        List<CupsPrinter> allPrinters = new ArrayList<>();
        for (CupsOperations ops : connections.values()) {
            try {
                if (ops.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                    allPrinters.addAll(ops.getPrinters());
                }
            } catch (CupsException e) {
                log.warn("Failed to get printers from {}: {}", ops.getServerId(), e.getMessage());
            }
        }
        return allPrinters;
    }

    @Override
    public CupsPrinter getPrinter(String printerName) throws CupsException {
        for (CupsOperations ops : connections.values()) {
            try {
                CupsPrinter printer = ops.getPrinter(printerName);
                if (printer != null) {
                    return printer;
                }
            } catch (CupsException e) {
                log.debug("Printer {} not found on {}", printerName, ops.getServerId());
            }
        }
        throw new CupsException(CupsException.ErrorCode.PRINTER_NOT_FOUND, "打印机不存在: " + printerName);
    }

    @Override
    public Map<String, Object> getPrinterInfo(CupsPrinter printer) {
        return getAvailableConnection().getPrinterInfo(printer);
    }

    @Override
    public int print(CupsPrinter printer, InputStream inputStream, String jobName, PrintOptions options) throws CupsException {
        return getAvailableConnection().print(printer, inputStream, jobName, options);
    }

    @Override
    public boolean cancelJob(CupsPrinter printer, Integer jobId) throws CupsException {
        return getAvailableConnection().cancelJob(printer, jobId);
    }

    @Override
    public boolean cancelJob(Integer jobId) throws CupsException {
        for (CupsOperations ops : connections.values()) {
            try {
                if (ops.cancelJob(jobId)) {
                    return true;
                }
            } catch (CupsException e) {
                log.debug("Job {} not found on {}", jobId, ops.getServerId());
            }
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> getJobs(CupsPrinter printer, String whichJobs) throws CupsException {
        return getAvailableConnection().getJobs(printer, whichJobs);
    }

    @Override
    public PrintJobAttributes getJobAttributes(Integer jobId) throws CupsException {
        for (CupsOperations ops : connections.values()) {
            try {
                PrintJobAttributes attrs = ops.getJobAttributes(jobId);
                if (attrs != null) {
                    return attrs;
                }
            } catch (CupsException e) {
                log.debug("Job {} not found on {}", jobId, ops.getServerId());
            }
        }
        throw new CupsException(CupsException.ErrorCode.JOB_NOT_FOUND, "打印任务不存在: " + jobId);
    }

    @Override
    public void close() {
        connections.values().forEach(CupsOperations::close);
        connections.clear();
        log.info("All CUPS connections closed");
    }

    @Override
    public ConnectionStatus getConnectionStatus() {
        return connections.values().stream()
            .anyMatch(ops -> ops.getConnectionStatus() == ConnectionStatus.CONNECTED)
            ? ConnectionStatus.CONNECTED
            : ConnectionStatus.DISCONNECTED;
    }

    /**
     * 服务器选择策略
     */
    public enum SelectionStrategy {
        /** 按优先级选择 */
        PRIORITY,
        /** 轮询 */
        ROUND_ROBIN,
        /** 随机选择 */
        RANDOM
    }
}