package com.powercess.printer_system.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

@Schema(description = "管理员更新用户请求")
public record AdminUserUpdateRequest(
    @Schema(description = "昵称")
    String nickname,

    @Schema(description = "用户组ID")
    Long groupId,

    @Schema(description = "钱包余额")
    @DecimalMin(value = "0.00", message = "钱包余额不能为负数")
    BigDecimal walletBalance,

    @Schema(description = "用户状态")
    @Min(value = 0, message = "状态值无效")
    Integer status
) {}