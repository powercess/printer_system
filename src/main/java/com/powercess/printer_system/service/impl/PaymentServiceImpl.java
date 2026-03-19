package com.powercess.printer_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powercess.printer_system.config.AppProperties;
import com.powercess.printer_system.dto.payment.PaymentCreateRequest;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.Payment;
import com.powercess.printer_system.entity.User;
import com.powercess.printer_system.entity.WalletTransaction;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.mapper.PaymentMapper;
import com.powercess.printer_system.mapper.UserMapper;
import com.powercess.printer_system.mapper.WalletTransactionMapper;
import com.powercess.printer_system.payment.QixiangPayClient;
import com.powercess.printer_system.payment.QixiangPayRequest;
import com.powercess.printer_system.payment.QixiangPayResponse;
import com.powercess.printer_system.service.PaymentService;
import com.powercess.printer_system.service.PrinterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final PrinterService printerService;
    private final AppProperties appProperties;
    private final QixiangPayClient qixiangPayClient;

    @Override
    @Transactional
    public Map<String, Object> createPayment(Long userId, PaymentCreateRequest request, String clientIp) {
        Order order = orderMapper.selectById(request.orderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权支付此订单");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(400, "订单状态不允许支付");
        }

        String paymentMethod = request.paymentMethod();
        if (!paymentMethod.equals("wechat") && !paymentMethod.equals("alipay") && !paymentMethod.equals("wallet")) {
            throw new BusinessException(400, "不支持的支付方式");
        }

        if (paymentMethod.equals("wallet")) {
            return handleWalletPayment(userId, order);
        }

        return handleThirdPartyPayment(userId, order, paymentMethod, clientIp);
    }

    private Map<String, Object> handleWalletPayment(Long userId, Order order) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        BigDecimal balance = user.getWalletBalance();
        BigDecimal amount = order.getFinalAmount();

        if (balance.compareTo(amount) < 0) {
            throw new BusinessException(400, "钱包余额不足");
        }

        BigDecimal balanceBefore = balance;
        BigDecimal balanceAfter = balance.subtract(amount);

        user.setWalletBalance(balanceAfter);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setUserId(userId);
        transaction.setType(1);
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedId("order_" + order.getId());
        transaction.setCreatedAt(LocalDateTime.now());
        walletTransactionMapper.insert(transaction);

        String transactionId = "WALLET_" + UUID.randomUUID().toString().substring(0, 16);
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPaymentMethod("wallet");
        payment.setPaymentType("order");
        payment.setTransactionId(transactionId);
        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        order.setStatus(1);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        Map<String, Object> printResult = printerService.executePrint(
            order.getId(), order.getPrinterName(), order.getFileId(),
            order.getColorMode(), order.getDuplex(), order.getPaperSize(), order.getCopies()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", transactionId);
        result.put("amount", amount);
        result.put("status", 1);
        result.put("printJobId", printResult.get("jobId"));
        result.put("printSuccess", printResult.get("success"));
        result.put("printMessage", printResult.get("message"));

        return result;
    }

    private Map<String, Object> handleThirdPartyPayment(Long userId, Order order, String paymentMethod, String clientIp) {
        String outTradeNo = "PRINT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + order.getId();
        String payType = paymentMethod.equals("wechat") ? "wxpay" : "alipay";

        String notifyUrl = appProperties.baseUrl() + "/api/payment/notify";
        // returnUrl 使用前端地址，支付完成后直接跳转到前端支付结果页
        String returnUrl = appProperties.frontendUrl() + "/payment-result?outTradeNo=" + outTradeNo;

        Map<String, Object> payResult = createThirdPartyPayment(
            outTradeNo, "打印订单" + order.getId(), order.getFinalAmount().toString(),
            notifyUrl, returnUrl, payType, clientIp
        );

        if (!Boolean.TRUE.equals(payResult.get("success"))) {
            throw new BusinessException(500, "支付下单失败: " + payResult.get("msg"));
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setPaymentType("order");
        payment.setMerchantId((String) payResult.get("trade_no"));
        payment.setTransactionId(outTradeNo);
        payment.setStatus(0);
        paymentMapper.insert(payment);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", outTradeNo);
        result.put("tradeNo", payResult.get("trade_no"));
        result.put("amount", order.getFinalAmount());
        result.put("status", 0);
        result.put("payurl", payResult.get("payurl"));
        result.put("qrcode", payResult.get("qrcode"));

        return result;
    }

    private Map<String, Object> createThirdPartyPayment(String outTradeNo, String name, String money,
                                                         String notifyUrl, String returnUrl, String payType, String clientIp) {
        QixiangPayRequest request = new QixiangPayRequest(
            outTradeNo,
            name,
            new BigDecimal(money),
            notifyUrl,
            returnUrl,
            payType,
            clientIp,
            "jump"
        );

        QixiangPayResponse response = qixiangPayClient.createPayment(request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", response.success());
        result.put("trade_no", response.tradeNo());
        result.put("payurl", response.payUrl());
        result.put("qrcode", response.qrcode());
        result.put("msg", response.msg());
        return result;
    }

    @Override
    public Payment getPaymentStatus(Long userId, String paymentId) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getTransactionId, paymentId);

        Payment payment = paymentMapper.selectOne(wrapper);
        if (payment == null) {
            throw new BusinessException(404, "支付记录不存在");
        }
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查询此支付");
        }

        return payment;
    }

    @Override
    @Transactional
    public Map<String, Object> handleNotify(Map<String, String> params) {
        log.info("Payment notify received: {}", params);

        // Verify signature
        if (!qixiangPayClient.verifyCallback(params)) {
            log.warn("Invalid signature in payment notify");
            return Map.of("status", "fail", "message", "invalid signature");
        }

        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String moneyStr = params.get("money");

        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            return Map.of("status", "success");
        }

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getTransactionId, outTradeNo);
        Payment payment = paymentMapper.selectOne(wrapper);

        if (payment == null) {
            log.warn("Payment not found for outTradeNo: {}", outTradeNo);
            return Map.of("status", "fail", "message", "payment not found");
        }

        if (payment.getStatus() == 1) {
            log.info("Payment already processed: {}", outTradeNo);
            return Map.of("status", "success");
        }

        // Verify amount
        BigDecimal callbackMoney = new BigDecimal(moneyStr);
        if (payment.getAmount().compareTo(callbackMoney) != 0) {
            log.warn("Amount mismatch for payment {}: expected {}, got {}",
                outTradeNo, payment.getAmount(), callbackMoney);
            return Map.of("status", "fail", "message", "amount mismatch");
        }

        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        payment.setMerchantId(params.get("trade_no"));
        paymentMapper.updateById(payment);

        // Handle based on payment type
        if ("wallet".equals(payment.getPaymentType())) {
            // Wallet recharge
            User user = userMapper.selectById(payment.getUserId());
            if (user != null) {
                BigDecimal balanceBefore = user.getWalletBalance();
                BigDecimal balanceAfter = balanceBefore.add(payment.getAmount());
                user.setWalletBalance(balanceAfter);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.updateById(user);

                WalletTransaction transaction = new WalletTransaction();
                transaction.setUserId(payment.getUserId());
                transaction.setType(0);
                transaction.setAmount(payment.getAmount());
                transaction.setBalanceBefore(balanceBefore);
                transaction.setBalanceAfter(balanceAfter);
                transaction.setRelatedId("recharge_" + outTradeNo);
                transaction.setCreatedAt(LocalDateTime.now());
                walletTransactionMapper.insert(transaction);
            }
        } else {
            // Order payment
            Order order = orderMapper.selectById(payment.getOrderId());
            if (order != null) {
                order.setStatus(1);
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);

                printerService.executePrint(
                    order.getId(), order.getPrinterName(), order.getFileId(),
                    order.getColorMode(), order.getDuplex(), order.getPaperSize(), order.getCopies()
                );
            }
        }

        return Map.of("status", "success");
    }

    @Override
    public String handleReturn(Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String money = params.get("money");

        StringBuilder url = new StringBuilder(appProperties.frontendUrl() + "/payment-result?");
        if (outTradeNo != null) {
            url.append("outTradeNo=").append(outTradeNo).append("&");
        }
        if (tradeStatus != null) {
            url.append("tradeStatus=").append(tradeStatus).append("&");
        }
        if (money != null) {
            url.append("money=").append(money);
        }

        return url.toString();
    }
}