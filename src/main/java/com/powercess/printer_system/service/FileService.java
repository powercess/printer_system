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
}