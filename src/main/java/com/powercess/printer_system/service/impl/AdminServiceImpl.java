package com.powercess.printer_system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.admin.AdminUserCreateRequest;
import com.powercess.printer_system.dto.admin.AdminUserUpdateRequest;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.entity.UserFile;
import com.powercess.printer_system.entity.UserGroup;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.mapper.UserFileMapper;
import com.powercess.printer_system.mapper.UserGroupMapper;
import com.powercess.printer_system.mapper.UserMapper;
import com.powercess.printer_system.service.AdminService;
import com.powercess.printer_system.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final UserFileMapper userFileMapper;
    private final OrderMapper orderMapper;

    private void checkAdminPermission(Long adminId) {
        log.trace("Checking admin permission for user: adminId={}", adminId);
        List<String> roles = StpUtil.getRoleList();
        if (!roles.contains("管理员")) {
            log.warn("Admin permission denied: adminId={}, roles={}", adminId, roles);
            throw new BusinessException(403, "权限不足");
        }
        log.trace("Admin permission verified: adminId={}", adminId);
    }

    @Override
    @Transactional
    public void createUser(Long adminId, AdminUserCreateRequest request) {
        checkAdminPermission(adminId);
        log.info("[Admin {}] Creating user: username={}", adminId, request.username());

        if (userMapper.findByUsername(request.username()).isPresent()) {
            log.warn("[Admin {}] Username already exists: {}", adminId, request.username());
            throw new BusinessException(400, "用户名已存在");
        }
        if (userMapper.findByEmail(request.email()).isPresent()) {
            log.warn("[Admin {}] Email already used: {}", adminId, request.email());
            throw new BusinessException(400, "邮箱已被使用");
        }

        UserGroup group = userGroupMapper.selectById(request.groupId() != null ? request.groupId() : 1L);
        if (group == null) {
            log.warn("[Admin {}] User group not found: groupId={}", adminId, request.groupId());
            throw new BusinessException(400, "用户组不存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(PasswordUtil.hash(request.password()));
        user.setNickname(request.nickname());
        user.setGroupId(request.groupId() != null ? request.groupId() : 1L);
        user.setWalletBalance(java.math.BigDecimal.ZERO);
        user.setCreatedAt(LocalDateTime.now());

        userMapper.insert(user);
        log.info("[Admin {}] User created: userId={}, username={}", adminId, user.getId(), request.username());
    }

    @Override
    public PageResult<User> getUsers(Long adminId, int page, int pageSize, String username, Long groupId) {
        checkAdminPermission(adminId);
        log.debug("[Admin {}] Getting users: page={}, pageSize={}, username={}, groupId={}", adminId, page, pageSize, username, groupId);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(User::getUsername, username);
        }
        if (groupId != null) {
            wrapper.eq(User::getGroupId, groupId);
        }
        wrapper.isNull(User::getDeletedAt);
        wrapper.orderByDesc(User::getCreatedAt);

        IPage<User> pageResult = userMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        pageResult.getRecords().forEach(user -> {
            UserGroup group = userGroupMapper.selectById(user.getGroupId());
            if (group != null) {
                user.setGroupName(group.getGroupName());
            }
        });

        log.debug("[Admin {}] Found {} users", adminId, pageResult.getTotal());
        return PageResult.of(pageResult.getTotal(), page, pageSize, pageResult.getRecords());
    }

    @Override
    @Transactional
    public void updateUser(Long adminId, Long userId, AdminUserUpdateRequest request) {
        checkAdminPermission(adminId);
        log.info("[Admin {}] Updating user: userId={}", adminId, userId);

        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> {
                log.warn("[Admin {}] User not found: userId={}", adminId, userId);
                return new BusinessException(404, "用户不存在");
            });

        if (request.groupId() != null) {
            UserGroup group = userGroupMapper.selectById(request.groupId());
            if (group == null) {
                log.warn("[Admin {}] User group not found: groupId={}", adminId, request.groupId());
                throw new BusinessException(400, "用户组不存在");
            }
            user.setGroupId(request.groupId());
        }

        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.walletBalance() != null) {
            log.info("[Admin {}] Updating wallet balance: userId={}, oldBalance={}, newBalance={}",
                adminId, userId, user.getWalletBalance(), request.walletBalance());
            user.setWalletBalance(request.walletBalance());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("[Admin {}] User updated: userId={}", adminId, userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long adminId, Long userId) {
        checkAdminPermission(adminId);
        log.warn("[Admin {}] Deleting user: userId={}", adminId, userId);

        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> {
                log.warn("[Admin {}] User not found for deletion: userId={}", adminId, userId);
                return new BusinessException(404, "用户不存在");
            });

        user.setDeletedAt(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("[Admin {}] User soft deleted: userId={}", adminId, userId);
    }

    @Override
    public PageResult<Map<String, Object>> getFiles(Long adminId, int page, int pageSize, Long userId) {
        checkAdminPermission(adminId);
        log.debug("[Admin {}] Getting files: page={}, pageSize={}, userId={}", adminId, page, pageSize, userId);

        LambdaQueryWrapper<UserFile> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(UserFile::getUserId, userId);
        }
        wrapper.isNull(UserFile::getDeletedAt);
        wrapper.orderByDesc(UserFile::getUploadTime);

        IPage<UserFile> pageResult = userFileMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        List<Map<String, Object>> items = pageResult.getRecords().stream()
            .map(file -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", file.getId());
                map.put("userId", file.getUserId());
                map.put("name", file.getDisplayName());
                map.put("pageCount", file.getPageCount());
                map.put("uploadTime", file.getUploadTime());
                map.put("blobId", file.getBlobId());
                return map;
            })
            .toList();

        log.debug("[Admin {}] Found {} files", adminId, pageResult.getTotal());
        return PageResult.of(pageResult.getTotal(), page, pageSize, items);
    }

    @Override
    public PageResult<Order> getOrders(Long adminId, int page, int pageSize, Integer status, Long userId) {
        checkAdminPermission(adminId);
        log.debug("[Admin {}] Getting orders: page={}, pageSize={}, status={}, userId={}", adminId, page, pageSize, status, userId);

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(Order::getUserId, userId);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> pageResult = orderMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        log.debug("[Admin {}] Found {} orders", adminId, pageResult.getTotal());
        return PageResult.of(pageResult.getTotal(), page, pageSize, pageResult.getRecords());
    }

    @Override
    public Map<String, Object> getStats(Long adminId) {
        checkAdminPermission(adminId);
        log.debug("[Admin {}] Getting stats", adminId);

        long totalUsers = userMapper.selectCount(
            new LambdaQueryWrapper<User>().isNull(User::getDeletedAt));
        long totalOrders = orderMapper.selectCount(new LambdaQueryWrapper<>());

        log.debug("[Admin {}] Stats: totalUsers={}, totalOrders={}", adminId, totalUsers, totalOrders);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalOrders", totalOrders);
        stats.put("totalRevenue", 0.0);
        stats.put("todayOrders", 0);
        stats.put("todayRevenue", 0.0);
        stats.put("activePrinters", 0);

        return stats;
    }
}