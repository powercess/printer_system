package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.config.AppProperties;
import com.powercess.printer_system.entity.FileEntity;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileMapper;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.service.PrinterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterServiceImpl implements PrinterService {

    private final AppProperties appProperties;
    private final FileMapper fileMapper;
    private final OrderMapper orderMapper;

    @Override
    public Map<String, Object> healthCheck() {
        Map<String, Object> result = new HashMap<>();
        try {
            result.put("status", "ok");
            result.put("cupsHost", appProperties.cups().host());
            result.put("cupsPort", appProperties.cups().port());
            result.put("printerCount", 0);
            result.put("printers", List.of());
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        return result;
    }

    @Override
    public Map<String, Object> getPrintersStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("items", List.of());
        return result;
    }

    @Override
    public Map<String, Object> getCupsPrinters() {
        Map<String, Object> result = new HashMap<>();
        result.put("items", List.of());
        result.put("total", 0);
        return result;
    }

    @Override
    public Map<String, Object> getPrinterDetail(String printerName) {
        throw new BusinessException(404, "打印机 " + printerName + " 不存在");
    }

    @Override
    public Map<String, Object> getPrintJobs(String printerName, String whichJobs) {
        Map<String, Object> result = new HashMap<>();
        result.put("items", List.of());
        result.put("total", 0);
        return result;
    }

    @Override
    public Map<String, Object> getPrintJobDetail(Integer jobId) {
        throw new BusinessException(404, "打印任务 " + jobId + " 不存在");
    }

    @Override
    public void cancelPrintJob(Integer jobId) {
        log.info("Canceling print job: {}", jobId);
    }

    @Override
    public Map<String, Object> print(Long userId, String printerName, String filePath, String title, Map<String, String> options) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("jobId", System.currentTimeMillis());
        result.put("message", "打印任务已提交");
        return result;
    }

    @Override
    public Map<String, Object> executePrint(Long orderId, String printerName, Long fileId, Integer colorMode, Integer duplex, String paperSize, Integer copies) {
        log.info("Executing print: orderId={}, printerName={}, fileId={}", orderId, printerName, fileId);

        Map<String, Object> result = new HashMap<>();

        try {
            FileEntity file = fileMapper.findByIdNotDeleted(fileId)
                .orElseThrow(() -> new BusinessException(404, "文件不存在"));

            String uploadDir = appProperties.upload().dir();
            Path filePath = Paths.get(uploadDir, file.getFilePath());

            log.info("File path: {}", filePath);

            Map<String, String> options = new HashMap<>();
            options.put("media", paperSize != null ? paperSize : "A4");
            options.put("ColorModel", colorMode != null && colorMode == 1 ? "RGB" : "Gray");
            options.put("sides", duplex != null && duplex == 1 ? "two-sided-long-edge" : "one-sided");
            options.put("copies", String.valueOf(copies != null ? copies : 1));

            result.put("success", true);
            result.put("jobId", System.currentTimeMillis());
            result.put("cupsPrinter", printerName != null ? printerName : "default");
            result.put("message", "打印任务已提交");

            Order order = orderMapper.selectById(orderId);
            if (order != null) {
                order.setStatus(2);
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
            }

        } catch (Exception e) {
            log.error("Print execution failed", e);
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
        }

        return result;
    }
}