package com.powercess.printer_system.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "创建支付请求")
public record PaymentCreateRequest(
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    Long orderId,

    @Schema(description = "支付方式: wechat/alipay/wallet", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "支付方式不能为空")
    String paymentMethod
) {}