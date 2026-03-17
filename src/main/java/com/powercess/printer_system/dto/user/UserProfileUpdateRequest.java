package com.powercess.printer_system.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "用户资料更新请求")
public record UserProfileUpdateRequest(
    @Schema(description = "昵称")
    @Size(max = 50, message = "昵称长度不能超过50")
    String nickname,

    @Schema(description = "头像URL")
    String avatarUrl,

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    String email
) {}