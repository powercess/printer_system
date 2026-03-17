package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("printers")
public class Printer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String location;

    private Integer status;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}