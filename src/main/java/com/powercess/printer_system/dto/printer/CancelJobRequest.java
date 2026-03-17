package com.powercess.printer_system.dto.printer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "取消打印任务请求")
public record CancelJobRequest(
    @Schema(description = "打印任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务ID不能为空")
    @Min(value = 1, message = "任务ID无效")
    Integer jobId
) {}