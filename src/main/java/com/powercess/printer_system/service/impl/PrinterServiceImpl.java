package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.config.AppProperties;
import com.powercess.printer_system.cups.CupsOperations;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.UserFile;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.mapper.UserFileMapper;
import com.powercess.printer_system.service.CupsClientService;
import com.powercess.printer_system.service.PdfConversionService;
import com.powercess.printer_system.service.PrinterService;
import com.powercess.printer_system.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.CupsPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterServiceImpl implements PrinterService {

    private final AppProperties appProperties;
    private final UserFileMapper userFileMapper;
    private final OrderMapper orderMapper;
    private final CupsClientService cupsClientService;
    private final StorageService storageService;
    private final PdfConversionService pdfConversionService;

    @Override
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean connected = cupsClientService.testConnection();
            result.put("status", connected ? "ok" : "error");
            result.put("cupsHost", appProperties.cups().host());
            result.put("cupsPort", appProperties.cups().port());
            if (connected) {
                List<CupsPrinter> printers = cupsClientService.getPrinters();
                result.put("printerCount", printers.size());
                result.put("printers", printers.stream().map(p -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("name", p.getName());
                    info.put("state", p.getState());
                    return info;
                }).toList());
            } else {
                result.put("printerCount", 0);
                result.put("printers", List.of());
                result.put("error", "无法连接到CUPS服务");
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
            result.put("printerCount", 0);
            result.put("printers", List.of());
        }
        return result;
    }

    @Override
    public Map<String, Object> getCupsServersStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, CupsOperations.ConnectionStatus> serverStatus = cupsClientService.getAllServerStatus();
            result.put("servers", serverStatus.entrySet().stream().map(entry -> {
                Map<String, Object> server = new HashMap<>();
                server.put("id", entry.getKey());
                server.put("status", entry.getValue().name());
                return server;
            }).toList());
            result.put("totalServers", serverStatus.size());
            long connectedCount = serverStatus.values().stream()
                .filter(s -> s == CupsOperations.ConnectionStatus.CONNECTED)
                .count();
            result.put("connectedServers", connectedCount);
            result.put("healthy", connectedCount > 0);
        } catch (Exception e) {
            log.error("Failed to get CUPS servers status: {}", e.getMessage());
            result.put("servers", List.of());
            result.put("totalServers", 0);
            result.put("connectedServers", 0);
            result.put("healthy", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getPrintersStatus() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<CupsPrinter> printers = cupsClientService.getPrinters();
            List<Map<String, Object>> items = printers.stream().map(printer -> {
                Map<String, Object> info = new HashMap<>();
                info.put("name", printer.getName());
                info.put("description", printer.getDescription());
                info.put("location", printer.getLocation());
                info.put("state", printer.getState());
                info.put("deviceUri", printer.getDeviceURI());
                return info;
            }).toList();
            result.put("items", items);
            result.put("total", items.size());
        } catch (Exception e) {
            log.error("Failed to get printers status: {}", e.getMessage());
            result.put("items", List.of());
            result.put("total", 0);
        }
        return result;
    }

    @Override
    public Map<String, Object> getCupsPrinters() {
        return getPrintersStatus();
    }

    @Override
    public Map<String, Object> getPrinterDetail(String printerName) {
        try {
            CupsPrinter printer = cupsClientService.getPrinter(printerName);
            if (printer == null) {
                throw new BusinessException(404, "打印机 " + printerName + " 不存在");
            }
            Map<String, Object> info = cupsClientService.getPrinterInfo(printer);
            // 获取打印任务
            List<Map<String, Object>> jobs = cupsClientService.getJobs(printer, "not-completed");
            info.put("jobs", jobs);
            info.put("jobCount", jobs.size());
            return info;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get printer detail: {}", e.getMessage());
            throw new BusinessException(500, "获取打印机详情失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getPrintJobs(String printerName, String whichJobs) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> jobs;
            if (printerName != null && !printerName.isEmpty()) {
                CupsPrinter printer = cupsClientService.getPrinter(printerName);
                if (printer != null) {
                    jobs = cupsClientService.getJobs(printer, whichJobs);
                } else {
                    jobs = List.of();
                }
            } else {
                // 获取所有打印机的任务
                jobs = new java.util.ArrayList<>();
                List<CupsPrinter> printers = cupsClientService.getPrinters();
                for (CupsPrinter printer : printers) {
                    jobs.addAll(cupsClientService.getJobs(printer, whichJobs));
                }
            }
            result.put("items", jobs);
            result.put("total", jobs.size());
        } catch (Exception e) {
            log.error("Failed to get print jobs: {}", e.getMessage());
            result.put("items", List.of());
            result.put("total", 0);
        }
        return result;
    }

    @Override
    public Map<String, Object> getPrintJobDetail(Integer jobId) {
        // cups4j 不直接支持查询单个任务，需要遍历
        try {
            List<CupsPrinter> printers = cupsClientService.getPrinters();
            for (CupsPrinter printer : printers) {
                List<Map<String, Object>> jobs = cupsClientService.getJobs(printer, "all");
                for (Map<String, Object> job : jobs) {
                    if (job.get("id").equals(jobId)) {
                        return job;
                    }
                }
            }
            throw new BusinessException(404, "打印任务 " + jobId + " 不存在");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get print job detail: {}", e.getMessage());
            throw new BusinessException(500, "获取打印任务详情失败: " + e.getMessage());
        }
    }

    @Override
    public void cancelPrintJob(Integer jobId) {
        log.info("Canceling print job: {}", jobId);
        boolean success = cupsClientService.cancelJob(jobId);
        if (!success) {
            throw new BusinessException(500, "取消打印任务失败");
        }
    }

    @Override
    public Map<String, Object> print(Long userId, String printerName, String filePath, String title, Map<String, String> options) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 通过 StorageService 获取文件内容
            byte[] content = storageService.downloadBytes(filePath);

            int copies = options.containsKey("copies") ? Integer.parseInt(options.get("copies")) : 1;
            String duplex = options.getOrDefault("sides", "one-sided");

            int jobId = cupsClientService.printBytes(printerName, content, title, copies, duplex);

            result.put("success", true);
            result.put("jobId", jobId);
            result.put("message", "打印任务已提交");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Print failed: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> executePrint(Long orderId, String printerName, Long fileId, Integer colorMode, Integer duplex, String paperSize, Integer copies) {
        log.info("Executing print: orderId={}, printerName={}, fileId={}", orderId, printerName, fileId);

        Map<String, Object> result = new HashMap<>();

        try {
            // 获取文件信息
            UserFile userFile = userFileMapper.findByIdNotDeletedWithBlob(fileId)
                .orElseThrow(() -> new BusinessException(404, "文件不存在"));

            String storagePath = userFile.getStoragePath();
            if (storagePath == null) {
                throw new BusinessException(500, "文件存储路径无效");
            }

            // 检查文件是否存在于原始存储位置
            String actualPath = storagePath;
            if (!storageService.exists(storagePath)) {
                // 文件不在原始位置，尝试在其他存储中查找
                log.info("File not found in original storage, searching other storages: {}", storagePath);
                String foundPath = storageService.findInAllStorages(storagePath);

                if (foundPath != null) {
                    log.info("File found in alternative storage: {}", foundPath);
                    actualPath = foundPath;
                } else {
                    // 所有存储都找不到文件，标记为丢失
                    log.error("File not found in any storage: fileId={}, path={}", fileId, storagePath);
                    throw new BusinessException(404, "文件已丢失，请联系管理员");
                }
            }

            // 通过 StorageService 获取文件内容
            byte[] content = storageService.downloadBytes(actualPath);
            log.info("File content retrieved from storage: filePath={}, size={}bytes", actualPath, content.length);

            // 转换为 PDF（所有文件都通过 Gotenberg 转换为 PDF 后打印）
            byte[] pdfContent = pdfConversionService.convertToPdf(content, userFile.getDisplayName());
            log.info("File converted to PDF for printing: originalSize={}, pdfSize={}bytes", content.length, pdfContent.length);

            // 获取打印机
            CupsPrinter printer = cupsClientService.getPrinter(printerName);
            if (printer == null) {
                throw new BusinessException(404, "打印机不存在: " + printerName);
            }

            // 构建打印选项
            Map<String, String> options = new HashMap<>();
            options.put("copies", String.valueOf(copies != null ? copies : 1));
            options.put("sides", duplex != null && duplex == 1 ? "two-sided-long-edge" : "one-sided");
            options.put("media", paperSize != null ? paperSize : "A4");
            options.put("ColorModel", colorMode != null && colorMode == 1 ? "RGB" : "Gray");

            // 打印 - 使用 printWithOptions 传入所有选项（发送转换后的 PDF）
            String jobName = "Order-" + orderId + "-" + userFile.getDisplayName();
            int jobId = cupsClientService.printWithOptions(printer, new ByteArrayInputStream(pdfContent), jobName, options);
            log.info("Print job submitted: orderId={}, jobId={}, printer={}, options={}", orderId, jobId, printerName, options);

            // Update order status and store CUPS job ID
            Order order = orderMapper.selectById(orderId);
            if (order != null) {
                order.setCupsJobId(jobId);
                order.setStatus(2); // 打印中
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
            }

            result.put("success", true);
            result.put("jobId", jobId);
            result.put("cupsPrinter", printerName);
            result.put("message", "打印任务已提交");

        } catch (BusinessException e) {
            // Update order status to failed on business exception
            updateOrderStatusOnFailure(orderId);
            log.error("Print execution failed (business error): orderId={}, error={}", orderId, e.getMessage());
            result.put("success", false);
            result.put("message", e.getMessage());
        } catch (Exception e) {
            // Update order status to failed on unexpected exception
            updateOrderStatusOnFailure(orderId);
            log.error("Print execution failed: orderId={}, error={}", orderId, e.getMessage(), e);
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
        }

        return result;
    }

    private void updateOrderStatusOnFailure(Long orderId) {
        if (orderId == null) return;
        try {
            Order order = orderMapper.selectById(orderId);
            if (order != null && order.getStatus() < 3) {
                order.setStatus(5); // failed
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("Order status updated to failed: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("Failed to update order status: {}", e.getMessage());
        }
    }
}