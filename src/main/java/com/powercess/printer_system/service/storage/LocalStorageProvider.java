package com.powercess.printer_system.service.storage;

import com.powercess.printer_system.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@Slf4j
public class LocalStorageProvider implements StorageProvider {

    private static final String PREFIX = "local/";
    private final Path rootPath;

    public LocalStorageProvider(String uploadDir) {
        this.rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        ensureDirectoryExists();
    }

    private void ensureDirectoryExists() {
        if (!Files.exists(rootPath)) {
            try {
                Files.createDirectories(rootPath);
                log.info("Created storage root directory: {}", rootPath);
            } catch (IOException e) {
                throw new BusinessException(500, "无法创建存储目录: " + rootPath);
            }
        }
        if (!Files.isWritable(rootPath)) {
            throw new BusinessException(500, "存储目录不可写: " + rootPath);
        }
    }

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        Path filePath = rootPath.resolve(key);
        Path parentDir = filePath.getParent();

        try {
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            Files.copy(content, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File saved to local storage: {}", filePath);
            return PREFIX + key;
        } catch (IOException e) {
            log.error("Failed to save file to local storage: {}", filePath, e);
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String key) {
        Path filePath = rootPath.resolve(key);
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "文件不存在: " + key);
        }
        try {
            return Files.newInputStream(filePath);
        } catch (IOException e) {
            log.error("Failed to read file from local storage: {}", filePath, e);
            throw new BusinessException(500, "文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadBytes(String key) {
        Path filePath = rootPath.resolve(key);
        if (!Files.exists(filePath)) {
            throw new BusinessException(404, "文件不存在: " + key);
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read file from local storage: {}", filePath, e);
            throw new BusinessException(500, "文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        Path filePath = rootPath.resolve(key);
        try {
            Files.deleteIfExists(filePath);
            log.debug("File deleted from local storage: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file from local storage: {}", filePath, e);
        }
    }

    @Override
    public String getPresignedUrl(String key, Duration expiration) {
        // 本地存储不支持预签名URL
        return null;
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(rootPath.resolve(key));
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }
}