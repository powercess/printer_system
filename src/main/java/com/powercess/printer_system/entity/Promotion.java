package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("promotions")
public class Promotion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Integer discountType;

    private BigDecimal discountValue;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}