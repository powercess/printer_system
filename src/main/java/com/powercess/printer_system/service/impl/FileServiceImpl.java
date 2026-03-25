package com.powercess.printer_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.entity.FileEntity;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileMapper;
import com.powercess.printer_system.service.FileService;
import com.powercess.printer_system.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final StorageService storageService;

    @Override
    @Transactional
    public Map<String, Object> upload(Long userId, MultipartFile file) {
        log.info("Uploading file for user: userId={}", userId);

        if (file.isEmpty()) {
            log.warn("Empty file upload attempt: userId={}", userId);
            throw new BusinessException(400, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unknown_file";
        }

        String fileExtension = getFileExtension(originalFilename);
        String fileType = getFileType(fileExtension);
        long fileSize = file.getSize();

        log.info("File info: name={}, extension={}, type={}, size={}bytes", originalFilename, fileExtension, fileType, fileSize);

        // 构建按日期分类的存储路径
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // 生成唯一文件名
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + "_" + sanitizeFilename(originalFilename);
        String relativePath = datePath + "/" + uniqueFilename;

        // 通过 StorageService 上传文件
        String storedPath;
        try {
            storedPath = storageService.upload(
                relativePath,
                file.getInputStream(),
                file.getSize(),
                file.getContentType()
            );
            log.info("File saved successfully: {} (storage type: {})", storedPath, storageService.getStorageType());
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", originalFilename, e);
            throw new BusinessException(500, "文件读取失败: " + e.getMessage());
        }

        // 计算页数
        int pageCount = 1;
        if ("pdf".equals(fileType)) {
            pageCount = countPdfPages(storedPath);
            log.debug("PDF page count: {}", pageCount);
        }

        // 保存文件信息到数据库
        FileEntity fileEntity = new FileEntity();
        fileEntity.setUserId(userId);
        fileEntity.setName(originalFilename);
        fileEntity.setFileType(fileType);
        fileEntity.setFileSize(fileSize);
        fileEntity.setPageCount(pageCount);
        fileEntity.setFilePath(storedPath);
        fileEntity.setUploadTime(now);

        try {
            fileMapper.insert(fileEntity);
        } catch (Exception e) {
            // 数据库保存失败，尝试删除已上传的文件
            log.error("Failed to save file info to database, cleaning up file: {}", storedPath, e);
            storageService.delete(storedPath);
            throw new BusinessException(500, "文件信息保存失败");
        }

        log.info("File uploaded successfully: fileId={}, userId={}, name={}, size={}bytes, pages={}",
            fileEntity.getId(), userId, originalFilename, fileSize, pageCount);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", fileEntity.getId());
        result.put("name", originalFilename);
        result.put("fileType", fileType);
        result.put("fileSize", fileSize);
        result.put("pageCount", pageCount);
        result.put("uploadTime", fileEntity.getUploadTime());
        result.put("filePath", storedPath);

        return result;
    }

    /**
     * 清理文件名，移除可能导致问题的字符
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // 移除路径分隔符和其他可能导致问题的字符
        return filename.replaceAll("[/\\\\:*?\"<>|]", "_");
    }

    @Override
    public PageResult<FileEntity> getMyFiles(Long userId, int page, int pageSize) {
        log.debug("Getting files for user: userId={}, page={}, pageSize={}", userId, page, pageSize);
        LambdaQueryWrapper<FileEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileEntity::getUserId, userId)
            .isNull(FileEntity::getDeletedAt)
            .orderByDesc(FileEntity::getUploadTime);

        IPage<FileEntity> pageResult = fileMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        log.debug("Found {} files for user {}", pageResult.getTotal(), userId);
        return PageResult.of(pageResult.getTotal(), page, pageSize, pageResult.getRecords());
    }

    @Override
    public FileEntity getFileDetail(Long userId, Long fileId) {
        log.debug("Getting file detail: userId={}, fileId={}", userId, fileId);
        FileEntity file = fileMapper.findByIdNotDeleted(fileId)
            .orElseThrow(() -> {
                log.warn("File not found: fileId={}", fileId);
                return new BusinessException(404, "文件不存在");
            });

        if (!file.getUserId().equals(userId)) {
            log.warn("File access denied: userId={}, fileId={}, ownerUserId={}", userId, fileId, file.getUserId());
            throw new BusinessException(403, "无权访问此文件");
        }

        return file;
    }

    @Override
    @Transactional
    public void deleteFile(Long userId, Long fileId) {
        log.info("Deleting file: userId={}, fileId={}", userId, fileId);
        FileEntity file = fileMapper.findByIdNotDeleted(fileId)
            .orElseThrow(() -> {
                log.warn("File not found for deletion: fileId={}", fileId);
                return new BusinessException(404, "文件不存在");
            });

        if (!file.getUserId().equals(userId)) {
            log.warn("File deletion denied: userId={}, fileId={}, ownerUserId={}", userId, fileId, file.getUserId());
            throw new BusinessException(403, "无权删除此文件");
        }

        // 删除存储中的文件
        storageService.delete(file.getFilePath());

        // 软删除数据库记录
        file.setDeletedAt(LocalDateTime.now());
        fileMapper.updateById(file);
        log.info("File soft deleted: fileId={}", fileId);
    }

    @Override
    public String getDownloadUrl(Long userId, Long fileId) {
        log.debug("Getting download URL: userId={}, fileId={}", userId, fileId);
        FileEntity file = fileMapper.findByIdNotDeleted(fileId)
            .orElseThrow(() -> {
                log.warn("File not found: fileId={}", fileId);
                return new BusinessException(404, "文件不存在");
            });

        if (!file.getUserId().equals(userId)) {
            log.warn("Download denied: userId={}, fileId={}, ownerUserId={}", userId, fileId, file.getUserId());
            throw new BusinessException(403, "无权下载此文件");
        }

        // 对象存储返回预签名URL
        String presignedUrl = storageService.getPresignedUrl(file.getFilePath(), null);
        if (presignedUrl != null) {
            return presignedUrl;
        }

        // 本地存储返回文件路径
        return file.getFilePath();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String getFileType(String extension) {
        return switch (extension) {
            case "pdf" -> "pdf";
            case "doc" -> "doc";
            case "docx" -> "docx";
            case "xls" -> "xls";
            case "xlsx" -> "xlsx";
            case "ppt" -> "ppt";
            case "pptx" -> "pptx";
            default -> "unknown";
        };
    }

    private int countPdfPages(String filePath) {
        // TODO: 实现 PDF 页数计算
        return 1;
    }
}