package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.entity.UserFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {

    /**
     * 上传文件（支持内容去重）
     * 如果文件内容已存在，只创建新的用户文件记录，不重复存储
     */
    Map<String, Object> upload(Long userId, MultipartFile file);

    /**
     * 获取用户的文件列表
     */
    PageResult<UserFile> getMyFiles(Long userId, int page, int pageSize);

    /**
     * 获取文件详情
     */
    UserFile getFileDetail(Long userId, Long fileId);

    /**
     * 删除文件（软删除，减少引用计数）
     */
    void deleteFile(Long userId, Long fileId);

    /**
     * 获取文件下载URL
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 下载URL（对象存储返回预签名URL，本地存储返回相对路径）
     */
    String getDownloadUrl(Long userId, Long fileId);

    /**
     * 根据ID获取文件（内部使用，包含 blob 信息）
     */
    UserFile getFileById(Long fileId);
}