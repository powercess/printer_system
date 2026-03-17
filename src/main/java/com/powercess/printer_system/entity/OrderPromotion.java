package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_promotions")
public class OrderPromotion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long promotionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}