package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("files")
public class FileEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String fileType;

    private Long fileSize;

    private Integer pageCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;

    private String filePath;

    @TableLogic(value = "NULL", delval = "NOW()")
    private LocalDateTime deletedAt;
}