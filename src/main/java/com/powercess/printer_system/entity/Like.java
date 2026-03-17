package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("likes")
public class Like {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shareId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}