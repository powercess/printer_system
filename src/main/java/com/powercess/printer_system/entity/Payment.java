package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payments")
public class Payment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private BigDecimal amount;

    private String paymentMethod;

    private String merchantId;

    private String transactionId;

    private String paymentType;

    private Integer status;

    private LocalDateTime paidAt;
}