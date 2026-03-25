package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface FileService {

    Map<String, Object> upload(Long userId, MultipartFile file);

    PageResult<FileEntity> getMyFiles(Long userId, int page, int pageSize);

    FileEntity getFileDetail(Long userId, Long fileId);

    void deleteFile(Long userId, Long fileId);

    /**
     * 获取文件下载URL
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 下载URL（对象存储返回预签名URL，本地存储返回相对路径）
     */
    String getDownloadUrl(Long userId, Long fileId);
}