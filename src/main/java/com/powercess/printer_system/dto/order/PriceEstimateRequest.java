package com.powercess.printer_system.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "价格估算请求")
public record PriceEstimateRequest(
    @Schema(description = "文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "文件ID不能为空")
    Long fileId,

    @Schema(description = "颜色模式: 0-黑白, 1-彩色")
    Integer colorMode,

    @Schema(description = "双面打印: 0-单面, 1-双面")
    Integer duplex,

    @Schema(description = "纸张大小")
    String paperSize,

    @Schema(description = "打印份数")
    @Min(value = 1, message = "打印份数至少为1")
    Integer copies,

    @Schema(description = "优惠活动ID")
    Long promotionId
) {
    public PriceEstimateRequest {
        colorMode = colorMode != null ? colorMode : 0;
        duplex = duplex != null ? duplex : 0;
        paperSize = paperSize != null ? paperSize : "A4";
        copies = copies != null ? copies : 1;
    }
}