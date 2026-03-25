package com.powercess.printer_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.entity.FileBlob;
import com.powercess.printer_system.entity.UserFile;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileBlobMapper;
import com.powercess.printer_system.mapper.UserFileMapper;
import com.powercess.printer_system.service.FileService;
import com.powercess.printer_system.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final UserFileMapper userFileMapper;
    private final FileBlobMapper fileBlobMapper;
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

        // 计算文件内容的 SHA-256 哈希值
        String contentHash;
        try {
            contentHash = calculateSHA256(file.getBytes());
            log.debug("Calculated content hash: {}", contentHash);
        } catch (IOException e) {
            log.error("Failed to read file content for hashing: {}", originalFilename, e);
            throw new BusinessException(500, "文件读取失败");
        }

        // 检查是否已存在相同内容的文件
        FileBlob existingBlob = fileBlobMapper.findByContentHash(contentHash).orElse(null);

        FileBlob blob;
        boolean isNewBlob = false;

        // 检查现有 blob 的物理文件是否真的存在
        if (existingBlob != null && existingBlob.getStoragePath() != null) {
            boolean fileExists = storageService.exists(existingBlob.getStoragePath());

            if (!fileExists) {
                // 物理文件不存在，尝试在其他存储中查找
                String foundPath = storageService.findInAllStorages(existingBlob.getStoragePath());
                if (foundPath != null) {
                    // 在其他存储中找到了，更新 blob 的存储路径
                    log.info("File found in alternative storage, updating blob path: blobId={}, oldPath={}, newPath={}",
                        existingBlob.getId(), existingBlob.getStoragePath(), foundPath);
                    existingBlob.setStoragePath(foundPath);
                    fileBlobMapper.updateById(existingBlob);
                    fileExists = true;
                }
            }

            if (fileExists) {
                // 物理文件存在，复用 blob
                blob = existingBlob;
                log.info("File content already exists, reusing blob: blobId={}, hash={}", blob.getId(), contentHash);
            } else {
                // 物理文件不存在，需要重新上传
                log.warn("Blob exists but physical file missing, re-uploading: blobId={}, hash={}", existingBlob.getId(), contentHash);
                isNewBlob = true;

                // 构建按日期分类的存储路径（使用哈希值作为文件名）
                LocalDateTime now = LocalDateTime.now();
                String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                String hashedFilename = contentHash.substring(0, 16) + "." + fileExtension;
                String relativePath = datePath + "/" + hashedFilename;

                // 通过 StorageService 上传文件
                String storedPath;
                try {
                    storedPath = storageService.upload(
                        relativePath,
                        file.getInputStream(),
                        file.getSize(),
                        file.getContentType()
                    );
                    log.info("File re-uploaded successfully: {} (storage type: {})", storedPath, storageService.getStorageType());
                } catch (IOException e) {
                    log.error("Failed to re-upload file: {}", originalFilename, e);
                    throw new BusinessException(500, "文件上传失败: " + e.getMessage());
                }

                // 更新现有 blob 的存储路径
                existingBlob.setStoragePath(storedPath);
                existingBlob.setFileSize(fileSize);
                existingBlob.setFileType(fileType);
                fileBlobMapper.updateById(existingBlob);
                blob = existingBlob;
            }
        } else {
            // 新文件内容，需要存储
            isNewBlob = true;

            // 构建按日期分类的存储路径（使用哈希值作为文件名）
            LocalDateTime now = LocalDateTime.now();
            String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String hashedFilename = contentHash.substring(0, 16) + "." + fileExtension;
            String relativePath = datePath + "/" + hashedFilename;

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

            // 创建新的 FileBlob 记录
            blob = new FileBlob();
            blob.setContentHash(contentHash);
            blob.setStoragePath(storedPath);
            blob.setFileSize(fileSize);
            blob.setFileType(fileType);
            blob.setRefCount(0);
            blob.setCreatedAt(LocalDateTime.now());

            fileBlobMapper.insert(blob);
            log.info("New blob created: blobId={}, hash={}", blob.getId(), contentHash);
        }

        // 计算页数（仅对新文件计算，复用文件不需要重新计算）
        int pageCount = 1;
        if (isNewBlob && "pdf".equals(fileType)) {
            pageCount = countPdfPages(blob.getStoragePath());
            log.debug("PDF page count: {}", pageCount);
        } else if (!isNewBlob) {
            // 对于复用的文件，可能需要重新计算页数（因为用户可能选择了不同的文件名但内容相同）
            // 这里简化处理，默认为1页，后续可以根据需要优化
            pageCount = 1;
        }

        // 创建 UserFile 记录
        UserFile userFile = new UserFile();
        userFile.setUserId(userId);
        userFile.setBlobId(blob.getId());
        userFile.setDisplayName(originalFilename);
        userFile.setPageCount(pageCount);
        userFile.setUploadTime(LocalDateTime.now());
        userFile.setCreatedAt(LocalDateTime.now());

        userFileMapper.insert(userFile);

        // 增加 blob 的引用计数
        fileBlobMapper.incrementRefCount(blob.getId());

        log.info("UserFile created: fileId={}, userId={}, blobId={}, name={}, size={}bytes, pages={}",
            userFile.getId(), userId, blob.getId(), originalFilename, fileSize, pageCount);

        Map<String, Object> result = new HashMap<>();
        result.put("fileId", userFile.getId());
        result.put("name", originalFilename);
        result.put("fileType", fileType);
        result.put("fileSize", fileSize);
        result.put("pageCount", pageCount);
        result.put("uploadTime", userFile.getUploadTime());
        result.put("storagePath", blob.getStoragePath());

        return result;
    }

    /**
     * 计算字节数组的 SHA-256 哈希值
     */
    private String calculateSHA256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public PageResult<UserFile> getMyFiles(Long userId, int page, int pageSize) {
        log.debug("Getting files for user: userId={}, page={}, pageSize={}", userId, page, pageSize);

        LambdaQueryWrapper<UserFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFile::getUserId, userId)
            .isNull(UserFile::getDeletedAt)
            .orderByDesc(UserFile::getUploadTime);

        IPage<UserFile> pageResult = userFileMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        // 填充 blob 信息
        for (UserFile userFile : pageResult.getRecords()) {
            FileBlob blob = fileBlobMapper.selectById(userFile.getBlobId());
            if (blob != null) {
                userFile.setFileSize(blob.getFileSize());
                userFile.setFileType(blob.getFileType());
                userFile.setStoragePath(blob.getStoragePath());
                userFile.setBlob(blob);
            }
        }

        log.debug("Found {} files for user {}", pageResult.getTotal(), userId);
        return PageResult.of(pageResult.getTotal(), page, pageSize, pageResult.getRecords());
    }

    @Override
    public UserFile getFileDetail(Long userId, Long fileId) {
        log.debug("Getting file detail: userId={}, fileId={}", userId, fileId);

        UserFile userFile = userFileMapper.findByIdNotDeletedWithBlob(fileId)
            .orElseThrow(() -> {
                log.warn("File not found: fileId={}", fileId);
                return new BusinessException(404, "文件不存在");
            });

        if (!userFile.getUserId().equals(userId)) {
            log.warn("File access denied: userId={}, fileId={}, ownerUserId={}", userId, fileId, userFile.getUserId());
            throw new BusinessException(403, "无权访问此文件");
        }

        return userFile;
    }

    @Override
    @Transactional
    public void deleteFile(Long userId, Long fileId) {
        log.info("Deleting file: userId={}, fileId={}", userId, fileId);

        UserFile userFile = userFileMapper.findByIdNotDeleted(fileId)
            .orElseThrow(() -> {
                log.warn("File not found for deletion: fileId={}", fileId);
                return new BusinessException(404, "文件不存在");
            });

        if (!userFile.getUserId().equals(userId)) {
            log.warn("File deletion denied: userId={}, fileId={}, ownerUserId={}", userId, fileId, userFile.getUserId());
            throw new BusinessException(403, "无权删除此文件");
        }

        Long blobId = userFile.getBlobId();

        // 软删除用户文件记录（Java 端控制时间，确保时区正确）
        userFileMapper.softDeleteById(fileId, LocalDateTime.now());

        // 减少引用计数（不删除 blob 记录，由后台任务清理）
        LambdaUpdateWrapper<FileBlob> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(FileBlob::getId, blobId)
            .gt(FileBlob::getRefCount, 0)
            .setSql("ref_count = GREATEST(0, ref_count - 1)");
        fileBlobMapper.update(null, updateWrapper);

        log.info("File soft deleted: fileId={}, blobId={}", fileId, blobId);
    }

    @Override
    public String getDownloadUrl(Long userId, Long fileId) {
        log.debug("Getting download URL: userId={}, fileId={}", userId, fileId);

        UserFile userFile = userFileMapper.findByIdNotDeletedWithBlob(fileId)
            .orElseThrow(() -> {
                log.warn("File not found: fileId={}", fileId);
                return new BusinessException(404, "文件不存在");
            });

        if (!userFile.getUserId().equals(userId)) {
            log.warn("Download denied: userId={}, fileId={}, ownerUserId={}", userId, fileId, userFile.getUserId());
            throw new BusinessException(403, "无权下载此文件");
        }

        String storagePath = userFile.getStoragePath();
        if (storagePath == null) {
            throw new BusinessException(500, "文件存储路径无效");
        }

        // 检查文件是否存在于原始存储位置
        if (storageService.exists(storagePath)) {
            log.debug("File found in original storage: {}", storagePath);
            return getDownloadUrlFromPath(storagePath);
        }

        // 文件不在原始位置，尝试在其他存储中查找
        log.info("File not found in original storage, searching other storages: {}", storagePath);
        String foundPath = storageService.findInAllStorages(storagePath);

        if (foundPath != null) {
            log.info("File found in alternative storage: {}", foundPath);
            // 更新 blob 的存储路径
            updateBlobStoragePath(userFile.getBlobId(), foundPath);
            return getDownloadUrlFromPath(foundPath);
        }

        // 所有存储都找不到文件，标记为丢失并提示用户
        log.error("File not found in any storage: fileId={}, path={}", fileId, storagePath);
        markFileAsMissing(userFile);
        throw new BusinessException(404, "文件已丢失，请联系管理员");
    }

    @Override
    public UserFile getFileById(Long fileId) {
        return userFileMapper.findByIdNotDeletedWithBlob(fileId).orElse(null);
    }

    /**
     * 从存储路径获取下载 URL
     */
    private String getDownloadUrlFromPath(String storagePath) {
        // 对象存储返回预签名URL
        String presignedUrl = storageService.getPresignedUrl(storagePath, null);
        if (presignedUrl != null) {
            return presignedUrl;
        }
        // 本地存储返回文件路径
        return storagePath;
    }

    /**
     * 更新 blob 的存储路径
     */
    private void updateBlobStoragePath(Long blobId, String newPath) {
        if (blobId == null || newPath == null) return;

        FileBlob blob = fileBlobMapper.selectById(blobId);
        if (blob != null && !newPath.equals(blob.getStoragePath())) {
            blob.setStoragePath(newPath);
            fileBlobMapper.updateById(blob);
            log.info("Updated blob storage path: blobId={}, newPath={}", blobId, newPath);
        }
    }

    /**
     * 标记文件为丢失状态（软删除）
     */
    private void markFileAsMissing(UserFile userFile) {
        userFileMapper.softDeleteById(userFile.getId(), LocalDateTime.now());
        log.warn("File marked as missing and soft deleted: fileId={}", userFile.getId());
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