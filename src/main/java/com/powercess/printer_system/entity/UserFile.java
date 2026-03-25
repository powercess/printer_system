package com.powercess.printer_system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户文件索引表
 * 用户视角的"文件"，每个用户可以有自己的文件名
 * 多个用户文件可以指向同一个 FileBlob（内容去重）
 */
@Data
@TableName("user_files")
public class UserFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 关联的 FileBlob ID
     */
    private Long blobId;

    /**
     * 用户看到的文件名（原始上传文件名）
     */
    private String displayName;

    /**
     * 页数（PDF 等）
     */
    private Integer pageCount;

    /**
     * 上传时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 软删除时间（NULL 表示未删除）
     */
    private LocalDateTime deletedAt;

    // ========== 关联字段（非数据库字段）==========

    /**
     * 关联的 FileBlob 对象（查询时填充）
     */
    @TableField(exist = false)
    private FileBlob blob;

    /**
     * 文件大小（从 blob 关联）
     */
    @TableField(exist = false)
    private Long fileSize;

    /**
     * 文件类型（从 blob 关联）
     */
    @TableField(exist = false)
    private String fileType;

    /**
     * 存储路径（从 blob 关联）
     */
    @TableField(exist = false)
    private String storagePath;
}