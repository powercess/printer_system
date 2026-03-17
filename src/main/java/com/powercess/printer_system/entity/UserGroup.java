package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_groups")
public class UserGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String groupName;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}