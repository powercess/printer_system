package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.order.OrderCreateRequest;
import com.powercess.printer_system.dto.order.PriceEstimateRequest;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "订单管理", description = "打印订单创建、查询、取消和价格估算接口")
@Slf4j
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping("/create")
    public Result<Map<String, Object>> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Creating order: fileId={}, printerName={}, copies={}", userId, request.fileId(), request.printerName(), request.copies());
        Map<String, Object> order = orderService.createOrder(userId, request);
        log.info("[{}] Order created: orderId={}, finalAmount={}", userId, order.get("orderId"), order.get("finalAmount"));
        return Result.success("订单创建成功", order);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/detail")
    public Result<Order> getOrderDetail(@Parameter(description = "订单ID") @RequestParam Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting order detail: orderId={}", userId, orderId);
        Order order = orderService.getOrderDetail(userId, orderId);
        return Result.success("获取成功", order);
    }

    @Operation(summary = "获取我的订单列表")
    @GetMapping("/list")
    public Result<PageResult<Order>> getMyOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting order list: page={}, pageSize={}, status={}", userId, page, pageSize, status);
        PageResult<Order> result = orderService.getMyOrders(userId, page, pageSize, status);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "取消订单")
    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@Parameter(description = "订单ID") @RequestParam Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Cancelling order: orderId={}", userId, orderId);
        orderService.cancelOrder(userId, orderId);
        log.info("[{}] Order cancelled: orderId={}", userId, orderId);
        return Result.success("订单已取消");
    }

    @Operation(summary = "估算价格")
    @PostMapping("/estimate")
    public Result<Map<String, BigDecimal>> estimatePrice(@Valid @RequestBody PriceEstimateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Estimating price: fileId={}, colorMode={}, copies={}", userId, request.fileId(), request.colorMode(), request.copies());
        Map<String, BigDecimal> price = orderService.estimatePrice(userId, request);
        return Result.success("估算成功", price);
    }
}