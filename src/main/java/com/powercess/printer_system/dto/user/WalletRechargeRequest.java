package com.powercess.printer_system.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "钱包充值请求")
public record WalletRechargeRequest(
    @Schema(description = "充值金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于0")
    BigDecimal amount,

    @Schema(description = "支付方式", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "支付方式不能为空")
    String paymentMethod
) {}