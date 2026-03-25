package com.powercess.printer_system.service;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    /**
     * 上传文件
     * @param relativePath 相对路径（不含前缀），如 "2024/03/24/xxx.pdf"
     * @param content 文件内容流
     * @param size 文件大小
     * @param contentType 内容类型
     * @return 完整存储路径（含前缀），如 "s3/2024/03/24/xxx.pdf"
     */
    String upload(String relativePath, InputStream content, long size, String contentType);

    /**
     * 下载文件
     * @param path 存储路径（含前缀）
     * @return 文件内容流
     */
    InputStream download(String path);

    /**
     * 下载文件为字节数组
     * @param path 存储路径（含前缀）
     * @return 文件内容字节数组
     */
    byte[] downloadBytes(String path);

    /**
     * 删除文件
     * @param path 存储路径（含前缀）
     */
    void delete(String path);

    /**
     * 获取预签名下载URL
     * @param path 存储路径（含前缀）
     * @param expiration 过期时间
     * @return 预签名URL，本地存储返回 null
     */
    String getPresignedUrl(String path, Duration expiration);

    /**
     * 判断是否启用对象存储
     */
    boolean isObjectStorageEnabled();

    /**
     * 获取当前存储类型
     */
    String getStorageType();
}