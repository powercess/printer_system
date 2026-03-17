package com.powercess.printer_system.dto.printer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

@Schema(description = "打印请求")
public record PrintRequest(
    @Schema(description = "打印机名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "打印机名称不能为空")
    String printerName,

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "文件路径不能为空")
    String filePath,

    @Schema(description = "打印任务标题")
    String title,

    @Schema(description = "打印选项")
    Map<String, String> options
) {
    public PrintRequest {
        title = title != null ? title : "打印任务";
    }
}