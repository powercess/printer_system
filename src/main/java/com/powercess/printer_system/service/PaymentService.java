package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.payment.PaymentCreateRequest;
import com.powercess.printer_system.entity.Payment;

import java.util.Map;

public interface PaymentService {

    Map<String, Object> createPayment(Long userId, PaymentCreateRequest request, String clientIp);

    Payment getPaymentStatus(Long userId, String paymentId);

    Map<String, Object> handleNotify(Map<String, String> params);

    String handleReturn(Map<String, String> params);

    // 主动查询支付状态并处理订单
    Map<String, Object> queryAndProcessPayment(Long userId, String outTradeNo);
}