package com.powercess.printer_system.dto.community;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "分享创建请求")
public record ShareCreateRequest(
    @Schema(description = "文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件ID不能为空")
    Long fileId
) {}