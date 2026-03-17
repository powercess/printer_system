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

    @Override
    @Transactional
    public Map<String, Object> createPayment(Long userId, PaymentCreateRequest request) {
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

        return handleThirdPartyPayment(userId, order, paymentMethod);
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
        payment.setTransactionId(transactionId);
        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        payment.setCreatedAt(LocalDateTime.now());
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

    private Map<String, Object> handleThirdPartyPayment(Long userId, Order order, String paymentMethod) {
        String outTradeNo = "PRINT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + order.getId();
        String payType = paymentMethod.equals("wechat") ? "wxpay" : "alipay";

        String notifyUrl = appProperties.baseUrl() + "/api/payment/notify";
        String returnUrl = appProperties.baseUrl() + "/api/payment/return";

        Map<String, Object> payResult = createThirdPartyPayment(
            outTradeNo, "打印订单" + order.getId(), order.getFinalAmount().toString(),
            notifyUrl, returnUrl, payType
        );

        if (!Boolean.TRUE.equals(payResult.get("success"))) {
            throw new BusinessException(500, "支付下单失败: " + payResult.get("msg"));
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getFinalAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setMerchantId((String) payResult.get("trade_no"));
        payment.setTransactionId(outTradeNo);
        payment.setStatus(0);
        payment.setCreatedAt(LocalDateTime.now());
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
                                                         String notifyUrl, String returnUrl, String payType) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("trade_no", "TRADE_" + System.currentTimeMillis());
        result.put("payurl", "https://example.com/pay?out_trade_no=" + outTradeNo);
        result.put("qrcode", "https://example.com/qrcode/" + outTradeNo);
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

        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");

        if (!"TRADE_SUCCESS".equals(tradeStatus)) {
            return Map.of("status", "success");
        }

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getTransactionId, outTradeNo);
        Payment payment = paymentMapper.selectOne(wrapper);

        if (payment == null || payment.getStatus() == 1) {
            return Map.of("status", "success");
        }

        payment.setStatus(1);
        payment.setPaidAt(LocalDateTime.now());
        payment.setMerchantId(params.get("trade_no"));
        paymentMapper.updateById(payment);

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

        return Map.of("status", "success");
    }

    @Override
    public String handleReturn(Map<String, String> params) {
        String outTradeNo = params.get("out_trade_no");
        String tradeStatus = params.get("trade_status");
        String money = params.get("money");

        StringBuilder url = new StringBuilder(appProperties.baseUrl() + "/payment-result.html?");
        if (outTradeNo != null) {
            url.append("out_trade_no=").append(outTradeNo).append("&");
        }
        if (tradeStatus != null) {
            url.append("trade_status=").append(tradeStatus).append("&");
        }
        if (money != null) {
            url.append("money=").append(money);
        }

        return url.toString();
    }
}