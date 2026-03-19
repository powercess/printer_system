package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.payment.PaymentCreateRequest;
import com.powercess.printer_system.entity.Payment;
import com.powercess.printer_system.service.PaymentService;
import com.powercess.printer_system.utils.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "支付管理", description = "支付创建、查询和回调接口")
@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "创建支付")
    @PostMapping("/create")
    public Result<Map<String, Object>> createPayment(@Valid @RequestBody PaymentCreateRequest request, HttpServletRequest httpRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = IpUtil.getClientIp(httpRequest);
        log.info("[{}] Creating payment: orderId={}, paymentMethod={}", userId, request.orderId(), request.paymentMethod());
        Map<String, Object> result = paymentService.createPayment(userId, request, clientIp);
        log.info("[{}] Payment created: paymentId={}, status={}", userId, result.get("paymentId"), result.get("status"));
        return Result.success("请前往支付页面完成支付", result);
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/status")
    public Result<Payment> getPaymentStatus(@Parameter(description = "支付ID") @RequestParam String paymentId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting payment status: paymentId={}", userId, paymentId);
        Payment payment = paymentService.getPaymentStatus(userId, paymentId);
        return Result.success("获取成功", payment);
    }

    @Operation(summary = "支付异步通知")
    @GetMapping("/notify")
    public String paymentNotifyGet(@RequestParam Map<String, String> params) {
        log.info("Payment notify (GET) received: out_trade_no={}", params.get("out_trade_no"));
        Map<String, Object> result = paymentService.handleNotify(params);
        log.info("Payment notify (GET) processed: status={}", result.get("status"));
        return "success".equals(result.get("status")) ? "success" : "fail";
    }

    @Operation(summary = "支付异步通知")
    @PostMapping("/notify")
    public String paymentNotifyPost(@RequestParam Map<String, String> params) {
        log.info("Payment notify (POST) received: out_trade_no={}", params.get("out_trade_no"));
        Map<String, Object> result = paymentService.handleNotify(params);
        log.info("Payment notify (POST) processed: status={}", result.get("status"));
        return "success".equals(result.get("status")) ? "success" : "fail";
    }

    @Operation(summary = "支付同步跳转")
    @GetMapping("/return")
    public String paymentReturnGet(@RequestParam Map<String, String> params) {
        log.info("Payment return (GET) received: out_trade_no={}", params.get("out_trade_no"));
        String redirectUrl = paymentService.handleReturn(params);
        log.debug("Redirecting to: {}", redirectUrl);
        return "redirect:" + redirectUrl;
    }

    @Operation(summary = "支付同步跳转")
    @PostMapping("/return")
    public String paymentReturnPost(@RequestParam Map<String, String> params) {
        log.info("Payment return (POST) received: out_trade_no={}", params.get("out_trade_no"));
        String redirectUrl = paymentService.handleReturn(params);
        log.debug("Redirecting to: {}", redirectUrl);
        return "redirect:" + redirectUrl;
    }

    @Operation(summary = "查询支付状态并处理订单")
    @GetMapping("/query")
    public Result<Map<String, Object>> queryPayment(@Parameter(description = "商户订单号") @RequestParam String outTradeNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Querying payment status: outTradeNo={}", userId, outTradeNo);
        Map<String, Object> result = paymentService.queryAndProcessPayment(userId, outTradeNo);
        log.info("[{}] Payment query result: status={}", userId, result.get("status"));
        return Result.success("查询成功", result);
    }
}