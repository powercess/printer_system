package com.powercess.printer_system.service.storage;

import java.io.InputStream;
import java.time.Duration;

public interface StorageProvider {

    /**
     * 上传文件
     * @param key 存储键（不含前缀）
     * @param content 文件内容流
     * @param size 文件大小
     * @param contentType 内容类型
     * @return 完整存储路径（含前缀）
     */
    String upload(String key, InputStream content, long size, String contentType);

    /**
     * 下载文件
     * @param key 存储键（不含前缀）
     * @return 文件内容流
     */
    InputStream download(String key);

    /**
     * 下载文件为字节数组
     * @param key 存储键（不含前缀）
     * @return 文件内容字节数组
     */
    byte[] downloadBytes(String key);

    /**
     * 删除文件
     * @param key 存储键（不含前缀）
     */
    void delete(String key);

    /**
     * 获取预签名下载URL
     * @param key 存储键（不含前缀）
     * @param expiration 过期时间
     * @return 预签名URL，不支持则返回 null
     */
    String getPresignedUrl(String key, Duration expiration);

    /**
     * 检查文件是否存在
     * @param key 存储键（不含前缀）
     */
    boolean exists(String key);

    /**
     * 获取存储类型前缀
     */
    String getPrefix();
}