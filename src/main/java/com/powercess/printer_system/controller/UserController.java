package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.user.*;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.service.UserService;
import com.powercess.printer_system.utils.IpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户管理", description = "用户注册、登录、资料管理和钱包相关接口")
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        log.debug("Processing register request: username={}", request.username());
        userService.register(request);
        log.info("User registered successfully: username={}", request.username());
        return Result.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody UserLoginRequest request) {
        log.debug("Processing login request: username={}", request.username());
        String token = userService.login(request);
        log.info("User logged in successfully: username={}", request.username());
        return Result.success("登录成功", Map.of("token", token));
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting user profile", userId);
        User user = userService.getProfile(userId);
        return Result.success("获取成功", user);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Updating profile: {}", userId, request);
        userService.updateProfile(userId, request);
        log.info("[{}] Profile updated successfully", userId);
        return Result.success("更新成功");
    }

    @Operation(summary = "获取钱包余额")
    @GetMapping("/wallet/balance")
    public Result<Map<String, BigDecimal>> getWalletBalance() {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting wallet balance", userId);
        BigDecimal balance = userService.getWalletBalance(userId);
        return Result.success("获取成功", Map.of("balance", balance));
    }

    @Operation(summary = "钱包充值")
    @PostMapping("/wallet/recharge")
    public Result<Map<String, Object>> recharge(@Valid @RequestBody WalletRechargeRequest request, HttpServletRequest httpRequest) {
        Long userId = StpUtil.getLoginIdAsLong();
        String clientIp = IpUtil.getClientIp(httpRequest);
        log.info("[{}] Creating wallet recharge: amount={}, paymentMethod={}", userId, request.amount(), request.paymentMethod());
        Map<String, Object> result = userService.createWalletRecharge(userId, request, clientIp);
        log.info("[{}] Wallet recharge order created: outTradeNo={}", userId, result.get("outTradeNo"));
        return Result.success("充值订单创建成功，请前往支付", result);
    }

    @Operation(summary = "查询充值状态")
    @GetMapping("/wallet/recharge/status")
    public Result<Map<String, Object>> getRechargeStatus(
            @Parameter(description = "商户订单号") @RequestParam String outTradeNo,
            @Parameter(description = "是否强制查询支付平台") @RequestParam(defaultValue = "true") boolean forceQuery) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting recharge status: outTradeNo={}, forceQuery={}", userId, outTradeNo, forceQuery);
        Map<String, Object> result = userService.getRechargeStatus(userId, outTradeNo, forceQuery);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取钱包流水")
    @GetMapping("/wallet/transactions")
    public Result<PageResult<Map<String, Object>>> getWalletTransactions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "交易类型") @RequestParam(required = false) Integer type) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting wallet transactions: page={}, pageSize={}, type={}", userId, page, pageSize, type);
        PageResult<Map<String, Object>> result = userService.getWalletTransactions(userId, page, pageSize, type);
        return Result.success("获取成功", result);
    }
}