package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.user.*;
import com.powercess.printer_system.entity.User;

import java.math.BigDecimal;
import java.util.Map;

public interface UserService {

    void register(UserRegisterRequest request);

    String login(UserLoginRequest request);

    User getProfile(Long userId);

    void updateProfile(Long userId, UserProfileUpdateRequest request);

    BigDecimal getWalletBalance(Long userId);

    Map<String, Object> createWalletRecharge(Long userId, WalletRechargeRequest request, String clientIp);

    Map<String, Object> getRechargeStatus(Long userId, String outTradeNo, boolean forceQuery);

    PageResult<Map<String, Object>> getWalletTransactions(Long userId, int page, int pageSize, Integer type);
}