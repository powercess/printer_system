package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.admin.AdminUserCreateRequest;
import com.powercess.printer_system.dto.admin.AdminUserUpdateRequest;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "管理员接口", description = "管理员用户管理、文件管理、订单管理和统计接口")
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "创建用户")
    @PostMapping("/user/create")
    public Result<Void> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.info("[Admin {}] Creating user: username={}", adminId, request.username());
        adminService.createUser(adminId, request);
        log.info("[Admin {}] User created: username={}", adminId, request.username());
        return Result.success("用户创建成功");
    }

    @Operation(summary = "获取用户列表")
    @GetMapping("/user/list")
    public Result<PageResult<User>> getUsers(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "用户组ID") @RequestParam(required = false) Long groupId) {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.debug("[Admin {}] Getting user list: page={}, pageSize={}, username={}, groupId={}", adminId, page, pageSize, username, groupId);
        PageResult<User> result = adminService.getUsers(adminId, page, pageSize, username, groupId);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/user/update")
    public Result<Void> updateUser(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.info("[Admin {}] Updating user: userId={}", adminId, userId);
        adminService.updateUser(adminId, userId, request);
        log.info("[Admin {}] User updated: userId={}", adminId, userId);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/user/delete")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @RequestParam Long userId) {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.warn("[Admin {}] Deleting user: userId={}", adminId, userId);
        adminService.deleteUser(adminId, userId);
        log.info("[Admin {}] User deleted: userId={}", adminId, userId);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取文件列表")
    @GetMapping("/file/list")
    public Result<PageResult<Map<String, Object>>> getFiles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.debug("[Admin {}] Getting file list: page={}, pageSize={}, userId={}", adminId, page, pageSize, userId);
        PageResult<Map<String, Object>> result = adminService.getFiles(adminId, page, pageSize, userId);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取订单列表")
    @GetMapping("/order/list")
    public Result<PageResult<Order>> getOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            @Parameter(description = "订单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId) {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.debug("[Admin {}] Getting order list: page={}, pageSize={}, status={}, userId={}", adminId, page, pageSize, status, userId);
        PageResult<Order> result = adminService.getOrders(adminId, page, pageSize, status, userId);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Long adminId = StpUtil.getLoginIdAsLong();
        log.debug("[Admin {}] Getting stats", adminId);
        Map<String, Object> stats = adminService.getStats(adminId);
        return Result.success("获取成功", stats);
    }
}