package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.payment.PaymentCreateRequest;
import com.powercess.printer_system.entity.Payment;
import com.powercess.printer_system.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "支付管理", description = "支付创建、查询和回调接口")
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付")
    @PostMapping("/create")
    public Result<Map<String, Object>> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = paymentService.createPayment(userId, request);
        return Result.success("请前往支付页面完成支付", result);
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/status")
    public Result<Payment> getPaymentStatus(@Parameter(description = "支付ID") @RequestParam String paymentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Payment payment = paymentService.getPaymentStatus(userId, paymentId);
        return Result.success("获取成功", payment);
    }

    @Operation(summary = "支付异步通知")
    @GetMapping("/notify")
    public String paymentNotifyGet(@RequestParam Map<String, String> params) {
        Map<String, Object> result = paymentService.handleNotify(params);
        return "success";
    }

    @Operation(summary = "支付异步通知")
    @PostMapping("/notify")
    public String paymentNotifyPost(@RequestParam Map<String, String> params) {
        Map<String, Object> result = paymentService.handleNotify(params);
        return "success";
    }

    @Operation(summary = "支付同步跳转")
    @GetMapping("/return")
    public String paymentReturnGet(@RequestParam Map<String, String> params) {
        return "redirect:" + paymentService.handleReturn(params);
    }

    @Operation(summary = "支付同步跳转")
    @PostMapping("/return")
    public String paymentReturnPost(@RequestParam Map<String, String> params) {
        return "redirect:" + paymentService.handleReturn(params);
    }
}