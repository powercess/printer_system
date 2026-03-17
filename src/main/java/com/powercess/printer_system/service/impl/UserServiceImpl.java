package com.powercess.printer_system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.user.*;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.entity.UserGroup;
import com.powercess.printer_system.entity.WalletTransaction;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.UserGroupMapper;
import com.powercess.printer_system.mapper.UserMapper;
import com.powercess.printer_system.mapper.WalletTransactionMapper;
import com.powercess.printer_system.service.UserService;
import com.powercess.printer_system.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    @Override
    @Transactional
    public void register(UserRegisterRequest request) {
        if (userMapper.findByUsername(request.username()).isPresent()) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (userMapper.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(PasswordUtil.hash(request.password()));
        user.setWalletBalance(BigDecimal.ZERO);
        user.setGroupId(1L);
        user.setCreatedAt(LocalDateTime.now());

        userMapper.insert(user);
    }

    @Override
    public String login(UserLoginRequest request) {
        User user = userMapper.findByUsername(request.username())
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        if (!PasswordUtil.verify(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        UserGroup group = userGroupMapper.selectById(user.getGroupId());
        if (group != null) {
            user.setGroupName(group.getGroupName());
        }

        return user;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userMapper.findByEmail(request.email()).isPresent()) {
                throw new BusinessException(400, "邮箱已被使用");
            }
            user.setEmail(request.email());
        }

        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public BigDecimal getWalletBalance(Long userId) {
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));
        return user.getWalletBalance();
    }

    @Override
    @Transactional
    public void recharge(Long userId, WalletRechargeRequest request) {
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> new BusinessException(401, "用户不存在"));

        BigDecimal balanceBefore = user.getWalletBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.amount());

        user.setWalletBalance(balanceAfter);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(0);
        transaction.setAmount(request.amount());
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedId("recharge_" + request.paymentMethod());
        transaction.setCreatedAt(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);
    }

    @Override
    public PageResult<Map<String, Object>> getWalletTransactions(Long userId, int page, int pageSize, Integer type) {
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletTransaction::getUserId, userId);
        if (type != null) {
            wrapper.eq(WalletTransaction::getType, type);
        }
        wrapper.orderByDesc(WalletTransaction::getCreatedAt);

        IPage<WalletTransaction> pageResult = walletTransactionMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        List<Map<String, Object>> items = pageResult.getRecords().stream()
            .map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("type", t.getType());
                map.put("amount", t.getAmount());
                map.put("balanceBefore", t.getBalanceBefore());
                map.put("balanceAfter", t.getBalanceAfter());
                map.put("relatedId", t.getRelatedId());
                map.put("createdAt", t.getCreatedAt());
                return map;
            })
            .toList();

        return PageResult.of(pageResult.getTotal(), page, pageSize, items);
    }
}