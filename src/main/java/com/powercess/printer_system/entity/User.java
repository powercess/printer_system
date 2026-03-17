package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String passwordHash;

    private String avatarUrl;

    private BigDecimal walletBalance;

    private Long groupId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;

    @TableField(exist = false)
    private String groupName;
}