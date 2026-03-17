package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wallet_transactions")
public class WalletTransaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer type;

    private BigDecimal amount;

    private BigDecimal balanceBefore;

    private BigDecimal balanceAfter;

    private String relatedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}