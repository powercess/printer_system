package com.powercess.printer_system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.config.AppProperties;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.user.*;
import com.powercess.printer_system.entity.Payment;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.entity.UserGroup;
import com.powercess.printer_system.entity.WalletTransaction;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.PaymentMapper;
import com.powercess.printer_system.mapper.UserGroupMapper;
import com.powercess.printer_system.mapper.UserMapper;
import com.powercess.printer_system.mapper.WalletTransactionMapper;
import com.powercess.printer_system.payment.QixiangPayClient;
import com.powercess.printer_system.payment.QixiangPayRequest;
import com.powercess.printer_system.payment.QixiangPayResponse;
import com.powercess.printer_system.service.UserService;
import com.powercess.printer_system.utils.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserGroupMapper userGroupMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final PaymentMapper paymentMapper;
    private final QixiangPayClient qixiangPayClient;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void register(UserRegisterRequest request) {
        log.debug("Processing registration: username={}", request.username());

        if (userMapper.findByUsername(request.username()).isPresent()) {
            log.warn("Registration failed - username exists: {}", request.username());
            throw new BusinessException(400, "用户名已存在");
        }
        if (userMapper.findByEmail(request.email()).isPresent()) {
            log.warn("Registration failed - email exists: {}", request.email());
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
        log.info("User registered: userId={}, username={}", user.getId(), request.username());
    }

    @Override
    public String login(UserLoginRequest request) {
        log.debug("Processing login: username={}", request.username());
        User user = userMapper.findByUsername(request.username())
            .orElseThrow(() -> {
                log.warn("Login failed - user not found: {}", request.username());
                return new BusinessException(401, "用户不存在");
            });

        if (!PasswordUtil.verify(request.password(), user.getPasswordHash())) {
            log.warn("Login failed - invalid password: username={}", request.username());
            throw new BusinessException(401, "用户名或密码错误");
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        log.info("User logged in: userId={}, username={}", user.getId(), request.username());
        return token;
    }

    @Override
    public User getProfile(Long userId) {
        log.debug("Getting profile: userId={}", userId);
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> {
                log.warn("Profile not found: userId={}", userId);
                return new BusinessException(401, "用户不存在");
            });

        UserGroup group = userGroupMapper.selectById(user.getGroupId());
        if (group != null) {
            user.setGroupName(group.getGroupName());
        }

        return user;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateRequest request) {
        log.debug("Updating profile: userId={}", userId);
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> {
                log.warn("User not found for profile update: userId={}", userId);
                return new BusinessException(401, "用户不存在");
            });

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userMapper.findByEmail(request.email()).isPresent()) {
                log.warn("Profile update failed - email already used: userId={}, email={}", userId, request.email());
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
        log.info("Profile updated: userId={}", userId);
    }

    @Override
    public BigDecimal getWalletBalance(Long userId) {
        log.debug("Getting wallet balance: userId={}", userId);
        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> {
                log.warn("User not found for wallet balance: userId={}", userId);
                return new BusinessException(401, "用户不存在");
            });
        log.debug("Wallet balance: userId={}, balance={}", userId, user.getWalletBalance());
        return user.getWalletBalance();
    }

    @Override
    @Transactional
    public Map<String, Object> createWalletRecharge(Long userId, WalletRechargeRequest request, String clientIp) {
        log.info("Creating wallet recharge: userId={}, amount={}, paymentMethod={}", userId, request.amount(), request.paymentMethod());

        User user = userMapper.findByIdNotDeleted(userId)
            .orElseThrow(() -> {
                log.warn("User not found for wallet recharge: userId={}", userId);
                return new BusinessException(401, "用户不存在");
            });

        String paymentMethod = request.paymentMethod();
        if (!paymentMethod.equals("wechat") && !paymentMethod.equals("alipay")) {
            log.warn("Unsupported payment method: {}", paymentMethod);
            throw new BusinessException(400, "不支持的支付方式");
        }

        String outTradeNo = "RCH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + userId;
        String payType = paymentMethod.equals("wechat") ? "wxpay" : "alipay";

        String notifyUrl = appProperties.baseUrl() + "/api/wallet/recharge/notify";
        String returnUrl = appProperties.frontendUrl() + "/payment-result?outTradeNo=" + outTradeNo;

        log.debug("Payment request: outTradeNo={}, notifyUrl={}, returnUrl={}", outTradeNo, notifyUrl, returnUrl);

        QixiangPayRequest payRequest = new QixiangPayRequest(
            outTradeNo,
            "钱包充值",
            request.amount(),
            notifyUrl,
            returnUrl,
            payType,
            clientIp,
            "jump"
        );

        QixiangPayResponse response = qixiangPayClient.createPayment(payRequest);

        if (!response.success()) {
            log.error("Payment creation failed: outTradeNo={}, msg={}", outTradeNo, response.msg());
            throw new BusinessException(500, "支付下单失败: " + response.msg());
        }

        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setAmount(request.amount());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentType("wallet");
        payment.setMerchantId(response.tradeNo());
        payment.setTransactionId(outTradeNo);
        payment.setStatus(0);
        paymentMapper.insert(payment);

        log.info("Wallet recharge order created: userId={}, outTradeNo={}, amount={}", userId, outTradeNo, request.amount());

        Map<String, Object> result = new HashMap<>();
        result.put("outTradeNo", outTradeNo);
        result.put("payUrl", response.payUrl());
        result.put("qrcode", response.qrcode());
        result.put("amount", request.amount());

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> getRechargeStatus(Long userId, String outTradeNo, boolean forceQuery) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getTransactionId, outTradeNo);
        wrapper.eq(Payment::getPaymentType, "wallet");
        Payment payment = paymentMapper.selectOne(wrapper);

        if (payment == null) {
            throw new BusinessException(404, "充值订单不存在");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查询此充值订单");
        }

        // 如果支付未完成且强制查询，则主动查询支付平台
        if (payment.getStatus() == 0 && forceQuery) {
            log.info("主动查询支付平台状态: {}", outTradeNo);
            var queryResponse = qixiangPayClient.queryPayment(outTradeNo);
            log.info("支付平台查询结果: {}", queryResponse);

            if (queryResponse.success() && "TRADE_SUCCESS".equals(queryResponse.tradeStatus())) {
                log.info("支付成功，更新数据库: {}", outTradeNo);
                // 更新支付状态
                payment.setStatus(1);
                payment.setPaidAt(LocalDateTime.now());
                if (queryResponse.tradeNo() != null) {
                    payment.setMerchantId(queryResponse.tradeNo());
                }
                paymentMapper.updateById(payment);

                // 更新用户余额
                User user = userMapper.selectById(userId);
                if (user != null) {
                    BigDecimal balanceBefore = user.getWalletBalance();
                    BigDecimal balanceAfter = balanceBefore.add(payment.getAmount());
                    user.setWalletBalance(balanceAfter);
                    user.setUpdatedAt(LocalDateTime.now());
                    userMapper.updateById(user);

                    // 记录交易流水
                    WalletTransaction transaction = new WalletTransaction();
                    transaction.setUserId(userId);
                    transaction.setType(0);
                    transaction.setAmount(payment.getAmount());
                    transaction.setBalanceBefore(balanceBefore);
                    transaction.setBalanceAfter(balanceAfter);
                    transaction.setRelatedId("recharge_" + outTradeNo);
                    transaction.setCreatedAt(LocalDateTime.now());
                    walletTransactionMapper.insert(transaction);

                    log.info("充值成功: userId={}, amount={}, balanceBefore={}, balanceAfter={}",
                        userId, payment.getAmount(), balanceBefore, balanceAfter);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("outTradeNo", outTradeNo);
        result.put("amount", payment.getAmount());
        result.put("status", payment.getStatus());
        result.put("paidAt", payment.getPaidAt());

        // 总是返回最新余额
        User user = userMapper.selectById(userId);
        if (user != null) {
            result.put("balance", user.getWalletBalance());
        }

        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getWalletTransactions(Long userId, int page, int pageSize, Integer type) {
        log.debug("Getting wallet transactions: userId={}, page={}, pageSize={}, type={}", userId, page, pageSize, type);
        LambdaQueryWrapper<WalletTransaction> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletTransaction::getUserId, userId);
        if (type != null) {
            wrapper.eq(WalletTransaction::getType, type);
        }
        wrapper.orderByDesc(WalletTransaction::getCreatedAt);

        IPage<WalletTransaction> pageResult = walletTransactionMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        log.debug("Found {} wallet transactions for user {}", pageResult.getTotal(), userId);

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