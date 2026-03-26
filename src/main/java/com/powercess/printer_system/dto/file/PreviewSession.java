package com.powercess.printer_system.dto.file;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预览会话
 * 用于临时存储转换后的 PDF 文件，等待用户确认
 */
@Data
@Builder
public class PreviewSession {

    /**
     * 会话ID（用于用户获取预览）
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 原始文件类型
     */
    private String originalFileType;

    /**
     * 原始文件大小
     */
    private Long originalFileSize;

    /**
     * 转换后的 PDF 数据
     */
    private byte[] pdfData;

    /**
     * PDF 文件大小
     */
    private Long pdfSize;

    /**
     * 是否需要转换
     */
    private boolean converted;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 会话状态: pending (等待确认), confirmed (已确认), expired (已过期)
     */
    private String status;

    /**
     * 页数
     */
    private Integer pageCount;
}