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

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileMapper fileMapper;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public Map<String, Object> upload(Long userId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String fileType = getFileType(fileExtension);

        LocalDateTime now = LocalDateTime.now();
        String relativePath = now.format(DateTimeFormatter.ofPattern("files/yyyy/MM/dd"));
        String uploadDir = appProperties.upload().dir();
        Path uploadPath = Paths.get(uploadDir, relativePath);

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new BusinessException(500, "创建上传目录失败");
        }

        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        Path filePath = uploadPath.resolve(uniqueFilename);

        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }

        long fileSize = file.getSize();
        int pageCount = 1;
        if ("pdf".equals(fileType)) {
            pageCount = countPdfPages(filePath.toString());
        }

        String fileRelativePath = relativePath + "/" + uniqueFilename;

        FileEntity fileEntity = new FileEntity();
        fileEntity.setUserId(userId);
        fileEntity.setName(originalFilename);
        fileEntity.setFileType(fileType);
        fileEntity.setFileSize(fileSize);
        fileEntity.setPageCount(pageCount);
        fileEntity.setFilePath(fileRelativePath);
        fileEntity.setUploadTime(now);

        fileMapper.insert(fileEntity);

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

    @Override
    public PageResult<FileEntity> getMyFiles(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<FileEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileEntity::getUserId, userId)
            .isNull(FileEntity::getDeletedAt)
            .orderByDesc(FileEntity::getUploadTime);

        IPage<FileEntity> pageResult = fileMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        return PageResult.of(pageResult.getTotal(), page, pageSize, pageResult.getRecords());
    }

    @Override
    public FileEntity getFileDetail(Long userId, Long fileId) {
        FileEntity file = fileMapper.findByIdNotDeleted(fileId)
            .orElseThrow(() -> new BusinessException(404, "文件不存在"));

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问此文件");
        }

        return file;
    }

    @Override
    @Transactional
    public void deleteFile(Long userId, Long fileId) {
        FileEntity file = fileMapper.findByIdNotDeleted(fileId)
            .orElseThrow(() -> new BusinessException(404, "文件不存在"));

        if (!file.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除此文件");
        }

        file.setDeletedAt(LocalDateTime.now());
        fileMapper.updateById(file);
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