package com.powercess.printer_system.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record QixiangPayRequest(
    @NotBlank(message = "商户订单号不能为空")
    String outTradeNo,

    @NotBlank(message = "商品名称不能为空")
    String name,

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    BigDecimal money,

    @NotBlank(message = "异步通知地址不能为空")
    String notifyUrl,

    String returnUrl,

    @NotBlank(message = "支付方式不能为空")
    String payType,

    String clientIp,

    String device
) {
    public QixiangPayRequest {
        device = device != null ? device : "jump";
    }
}