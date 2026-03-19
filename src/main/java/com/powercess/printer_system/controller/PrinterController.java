package com.powercess.printer_system.controller;

import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.service.PrinterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "打印机管理", description = "打印机状态、CUPS连接和打印任务接口")
@Slf4j
@RestController
@RequestMapping("/api/printer")
@RequiredArgsConstructor
public class PrinterController {

    private final PrinterService printerService;

    @Operation(summary = "CUPS健康检查")
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        log.debug("Performing CUPS health check");
        return printerService.healthCheck();
    }

    @Operation(summary = "获取所有CUPS服务器状态")
    @GetMapping("/servers")
    public Result<Map<String, Object>> getCupsServersStatus() {
        log.debug("Getting CUPS servers status");
        Map<String, Object> result = printerService.getCupsServersStatus();
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取打印机状态")
    @GetMapping("/status")
    public Result<Map<String, Object>> getPrintersStatus() {
        log.debug("Getting printers status");
        Map<String, Object> result = printerService.getPrintersStatus();
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取CUPS打印机列表")
    @GetMapping("/cups/list")
    public Result<Map<String, Object>> getCupsPrinters() {
        log.debug("Getting CUPS printers list");
        Map<String, Object> result = printerService.getCupsPrinters();
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取打印机详情")
    @GetMapping("/cups/{printerName}")
    public Result<Map<String, Object>> getPrinterDetail(
            @Parameter(description = "打印机名称") @PathVariable String printerName) {
        log.debug("Getting printer detail: printerName={}", printerName);
        Map<String, Object> result = printerService.getPrinterDetail(printerName);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取打印任务列表")
    @GetMapping("/cups/jobs")
    public Result<Map<String, Object>> getPrintJobs(
            @Parameter(description = "打印机名称") @RequestParam(required = false) String printerName,
            @Parameter(description = "任务类型") @RequestParam(defaultValue = "not-completed") String whichJobs) {
        log.debug("Getting print jobs: printerName={}, whichJobs={}", printerName, whichJobs);
        Map<String, Object> result = printerService.getPrintJobs(printerName, whichJobs);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取打印任务详情")
    @GetMapping("/cups/jobs/{jobId}")
    public Result<Map<String, Object>> getPrintJobDetail(
            @Parameter(description = "任务ID") @PathVariable Integer jobId) {
        log.debug("Getting print job detail: jobId={}", jobId);
        Map<String, Object> result = printerService.getPrintJobDetail(jobId);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "取消打印任务")
    @PostMapping("/cups/jobs/cancel")
    public Result<Void> cancelPrintJob(@Parameter(description = "任务ID") @RequestParam Integer jobId) {
        log.info("Cancelling print job: jobId={}", jobId);
        printerService.cancelPrintJob(jobId);
        log.info("Print job cancelled: jobId={}", jobId);
        return Result.success("打印任务已取消");
    }
}