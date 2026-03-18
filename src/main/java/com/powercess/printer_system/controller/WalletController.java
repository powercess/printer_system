package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.user.WalletRechargeRequest;
import com.powercess.printer_system.service.PaymentService;
import com.powercess.printer_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "钱包管理", description = "钱包充值、查询等接口")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final UserService userService;
    private final PaymentService paymentService;

    @Operation(summary = "创建钱包充值订单")
    @PostMapping("/recharge")
    public Result<Map<String, Object>> createRecharge(@Valid @RequestBody WalletRechargeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = userService.createWalletRecharge(userId, request);
        return Result.success("充值订单创建成功，请前往支付", result);
    }

    @Operation(summary = "查询充值订单状态")
    @GetMapping("/recharge/status")
    public Result<Map<String, Object>> getRechargeStatus(
        @Parameter(description = "商户订单号") @RequestParam String outTradeNo) {
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> result = userService.getRechargeStatus(userId, outTradeNo);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "充值支付异步通知")
    @GetMapping("/recharge/notify")
    public String rechargeNotifyGet(@RequestParam Map<String, String> params) {
        Map<String, Object> result = paymentService.handleNotify(params);
        return "success".equals(result.get("status")) ? "success" : "fail";
    }

    @Operation(summary = "充值支付异步通知")
    @PostMapping("/recharge/notify")
    public String rechargeNotifyPost(@RequestParam Map<String, String> params) {
        Map<String, Object> result = paymentService.handleNotify(params);
        return "success".equals(result.get("status")) ? "success" : "fail";
    }
}