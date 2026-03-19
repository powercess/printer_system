package com.powercess.printer_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.config.AppProperties;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.entity.FileEntity;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileMapper;
import com.powercess.printer_system.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final AppProperties appProperties;

    /**
     * 初始化上传根目录，确保目录存在且可写
     */
    private Path ensureUploadRootDirectory() {
        String uploadDir = appProperties.upload().dir();
        Path rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        // 如果目录不存在，创建它
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectories(rootPath);
                log.info("Created upload root directory: {}", rootPath);
            } catch (IOException e) {
                log.error("Failed to create upload root directory: {}", rootPath, e);
                throw new BusinessException(500, "无法创建上传目录: " + rootPath);
            }
        }

        // 检查目录是否可写
        if (!Files.isWritable(rootPath)) {
            log.error("Upload directory is not writable: {}", rootPath);
            throw new BusinessException(500, "上传目录不可写: " + rootPath);
        }

        log.debug("Upload root directory ready: {}", rootPath);
        return rootPath;
    }

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

        // 确保上传根目录存在
        Path rootPath = ensureUploadRootDirectory();

        // 构建按日期分类的子目录
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadPath = rootPath.resolve(datePath);

        // 创建日期子目录
        try {
            Files.createDirectories(uploadPath);
            log.debug("Created upload subdirectory: {}", uploadPath);
        } catch (IOException e) {
            log.error("Failed to create upload subdirectory: {}", uploadPath, e);
            throw new BusinessException(500, "创建上传目录失败: " + e.getMessage());
        }

        // 生成唯一文件名
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + "_" + sanitizeFilename(originalFilename);
        Path filePath = uploadPath.resolve(uniqueFilename);

        // 保存文件
        try {
            file.transferTo(filePath);
            log.info("File saved successfully: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save file: {}", filePath, e);
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.error("File upload state error: {}", filePath, e);
            throw new BusinessException(500, "文件上传状态错误: " + e.getMessage());
        }

        // 计算页数
        int pageCount = 1;
        if ("pdf".equals(fileType)) {
            pageCount = countPdfPages(filePath.toString());
            log.debug("PDF page count: {}", pageCount);
        }

        // 构建相对路径（用于数据库存储）
        String fileRelativePath = datePath + "/" + uniqueFilename;

        // 保存文件信息到数据库
        FileEntity fileEntity = new FileEntity();
        fileEntity.setUserId(userId);
        fileEntity.setName(originalFilename);
        fileEntity.setFileType(fileType);
        fileEntity.setFileSize(fileSize);
        fileEntity.setPageCount(pageCount);
        fileEntity.setFilePath(fileRelativePath);
        fileEntity.setUploadTime(now);

        try {
            fileMapper.insert(fileEntity);
        } catch (Exception e) {
            // 数据库保存失败，尝试删除已上传的文件
            log.error("Failed to save file info to database, cleaning up file: {}", filePath, e);
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException deleteError) {
                log.warn("Failed to cleanup file after database error: {}", filePath);
            }
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
        result.put("filePath", fileRelativePath);

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

        file.setDeletedAt(LocalDateTime.now());
        fileMapper.updateById(file);
        log.info("File soft deleted: fileId={}", fileId);
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
        return 1;
    }
}