package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.config.StorageProperties;
import com.powercess.printer_system.service.StorageService;
import com.powercess.printer_system.service.storage.LocalStorageProvider;
import com.powercess.printer_system.service.storage.S3StorageProvider;
import com.powercess.printer_system.service.storage.StorageProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Duration;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {

    private static final String LOCAL_PREFIX = "local/";
    private static final String S3_PREFIX = "s3/";

    private final StorageProperties storageProperties;
    private StorageProvider activeProvider;
    private LocalStorageProvider localStorageProvider;
    private S3StorageProvider s3StorageProvider;

    public StorageServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @PostConstruct
    public void init() {
        // 本地存储始终初始化（用于读取旧数据和本地存储模式）
        localStorageProvider = new LocalStorageProvider(storageProperties.local().dir());

        // 根据配置初始化主存储
        if (storageProperties.isS3Enabled()) {
            s3StorageProvider = new S3StorageProvider(storageProperties.s3());
            activeProvider = s3StorageProvider;
            log.info("Storage initialized with S3 provider");
        } else {
            activeProvider = localStorageProvider;
            log.info("Storage initialized with local provider");
        }
    }

    @Override
    public String upload(String relativePath, InputStream content, long size, String contentType) {
        return activeProvider.upload(relativePath, content, size, contentType);
    }

    @Override
    public InputStream download(String path) {
        StorageProvider provider = resolveProvider(path);
        String key = stripPrefix(path);
        return provider.download(key);
    }

    @Override
    public byte[] downloadBytes(String path) {
        StorageProvider provider = resolveProvider(path);
        String key = stripPrefix(path);
        return provider.downloadBytes(key);
    }

    @Override
    public String getPresignedUrl(String path, Duration expiration) {
        StorageProvider provider = resolveProvider(path);
        String key = stripPrefix(path);
        return provider.getPresignedUrl(key, expiration);
    }

    @Override
    public boolean isObjectStorageEnabled() {
        return storageProperties.isS3Enabled();
    }

    @Override
    public String getStorageType() {
        return storageProperties.isS3Enabled() ? "s3" : "local";
    }

    @Override
    public boolean exists(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        try {
            StorageProvider provider = resolveProvider(path);
            String key = stripPrefix(path);
            return provider.exists(key);
        } catch (Exception e) {
            log.warn("Failed to check file existence: {}", path, e);
            return false;
        }
    }

    @Override
    public String findInAllStorages(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        // 优先检查当前活动的存储
        String activePrefix = activeProvider.getPrefix();
        String activePath = activePrefix + key;
        if (activeProvider.exists(key)) {
            log.debug("File found in active storage: {}", activePath);
            return activePath;
        }

        // 如果当前是 S3，也检查本地存储
        if (s3StorageProvider != null && localStorageProvider != null) {
            if (activeProvider == s3StorageProvider) {
                // 当前是 S3，检查本地
                String localPath = LOCAL_PREFIX + key;
                if (localStorageProvider.exists(key)) {
                    log.info("File found in local storage (fallback): {}", localPath);
                    return localPath;
                }
            } else {
                // 当前是本地，检查 S3
                String s3Path = S3_PREFIX + key;
                if (s3StorageProvider.exists(key)) {
                    log.info("File found in S3 storage (fallback): {}", s3Path);
                    return s3Path;
                }
            }
        }

        // 也检查带前缀的原始路径
        if (key.startsWith(LOCAL_PREFIX)) {
            String strippedKey = stripPrefix(key);
            if (localStorageProvider.exists(strippedKey)) {
                return key;
            }
        }
        if (key.startsWith(S3_PREFIX)) {
            String strippedKey = stripPrefix(key);
            if (s3StorageProvider != null && s3StorageProvider.exists(strippedKey)) {
                return key;
            }
        }

        log.debug("File not found in any storage: {}", key);
        return null;
    }

    @Override
    public boolean isStorageAvailable(String storageType) {
        if ("s3".equalsIgnoreCase(storageType)) {
            return s3StorageProvider != null;
        }
        if ("local".equalsIgnoreCase(storageType)) {
            return localStorageProvider != null;
        }
        return false;
    }

    /**
     * 根据路径前缀解析对应的存储提供者
     */
    private StorageProvider resolveProvider(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("存储路径不能为空");
        }

        if (path.startsWith(S3_PREFIX)) {
            if (s3StorageProvider == null) {
                throw new IllegalStateException("S3存储未配置，无法访问: " + path);
            }
            return s3StorageProvider;
        }

        // 默认使用本地存储（包括 local/ 前缀和无前缀的旧数据）
        return localStorageProvider;
    }

    /**
     * 去掉路径前缀
     */
    private String stripPrefix(String path) {
        if (path.startsWith(LOCAL_PREFIX)) {
            return path.substring(LOCAL_PREFIX.length());
        }
        if (path.startsWith(S3_PREFIX)) {
            return path.substring(S3_PREFIX.length());
        }
        // 旧数据无前缀，直接返回
        return path;
    }
}