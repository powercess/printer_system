package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.file.PreviewSession;
import org.springframework.web.multipart.MultipartFile;

/**
 * 预览会话服务接口
 */
public interface PreviewSessionService {

    /**
     * 创建预览会话
     * @param userId 用户ID
     * @param file 上传的文件
     * @return 预览会话
     */
    PreviewSession createPreview(Long userId, MultipartFile file);

    /**
     * 获取预览会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 预览会话
     */
    PreviewSession getPreview(String sessionId, Long userId);

    /**
     * 确认预览并转换为正式文件
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 文件ID
     */
    Long confirmPreview(String sessionId, Long userId);

    /**
     * 取消预览会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void cancelPreview(String sessionId, Long userId);

    /**
     * 清理过期的预览会话
     */
    void cleanupExpiredSessions();

    /**
     * 获取预览PDF数据
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return PDF数据
     */
    byte[] getPdfData(String sessionId, Long userId);
}