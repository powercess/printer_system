package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.user.*;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Tag(name = "用户管理", description = "用户注册、登录、资料管理和钱包相关接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
        return Result.success("注册成功");
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<Map<String, String>> login(@Valid @RequestBody UserLoginRequest request) {
        String token = userService.login(request);
        return Result.success("登录成功", Map.of("token", token));
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/profile")
    public Result<User> getProfile() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userService.getProfile(userId);
        return Result.success("获取成功", user);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.updateProfile(userId, request);
        return Result.success("更新成功");
    }

    @Operation(summary = "获取钱包余额")
    @GetMapping("/wallet/balance")
    public Result<Map<String, BigDecimal>> getWalletBalance() {
        Long userId = StpUtil.getLoginIdAsLong();
        BigDecimal balance = userService.getWalletBalance(userId);
        return Result.success("获取成功", Map.of("balance", balance));
    }

    @Operation(summary = "钱包充值")
    @PostMapping("/wallet/recharge")
    public Result<Map<String, BigDecimal>> recharge(@Valid @RequestBody WalletRechargeRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        userService.recharge(userId, request);
        BigDecimal balance = userService.getWalletBalance(userId);
        return Result.success("充值成功", Map.of("balanceAfter", balance));
    }

    @Operation(summary = "获取钱包流水")
    @GetMapping("/wallet/transactions")
    public Result<PageResult<Map<String, Object>>> getWalletTransactions(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "交易类型") @RequestParam(required = false) Integer type) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<Map<String, Object>> result = userService.getWalletTransactions(userId, page, pageSize, type);
        return Result.success("获取成功", result);
    }
}