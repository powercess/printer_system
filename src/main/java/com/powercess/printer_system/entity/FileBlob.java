package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件内容存储表（内容寻址）
 * 相同内容的文件只存储一份，通过哈希值去重
 */
@Data
@TableName("file_blobs")
public class FileBlob {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * SHA-256 哈希值，用于去重
     */
    private String contentHash;

    /**
     * 物理存储路径（含前缀，如 s3/2024/03/24/xxx.pdf）
     */
    private String storagePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型（pdf, doc, docx 等）
     */
    private String fileType;

    /**
     * 引用计数，记录有多少 user_files 指向此 blob
     */
    private Integer refCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}