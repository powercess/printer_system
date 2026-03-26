package com.powercess.printer_system.service.storage;

import com.powercess.printer_system.config.StorageProperties;
import com.powercess.printer_system.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
public class S3StorageProvider implements StorageProvider {

    private static final String PREFIX = "s3/";
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final int presignedUrlExpiration;

    public S3StorageProvider(StorageProperties.S3 config) {
        this.bucket = config.bucket();
        this.presignedUrlExpiration = config.presignedUrlExpiration();

        var credentials = AwsBasicCredentials.create(config.accessKey(), config.secretKey());

        var clientBuilder = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(config.region()));

        if (config.pathStyle()) {
            clientBuilder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());
        }

        if (config.endpoint() != null && !config.endpoint().isBlank()) {
            clientBuilder.endpointOverride(URI.create(config.endpoint()));
        }

        this.s3Client = clientBuilder.build();

        var presignerBuilder = S3Presigner.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(config.region()));

        if (config.pathStyle()) {
            presignerBuilder.serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build());
        }

        if (config.endpoint() != null && !config.endpoint().isBlank()) {
            presignerBuilder.endpointOverride(URI.create(config.endpoint()));
        }

        this.presigner = presignerBuilder.build();

        log.info("S3 storage provider initialized: bucket={}, region={}", bucket, config.region());
    }

    @Override
    public String upload(String key, InputStream content, long size, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();

            s3Client.putObject(request, RequestBody.fromInputStream(content, size));
            log.debug("File uploaded to S3: bucket={}, key={}", bucket, key);
            return PREFIX + key;
        } catch (Exception e) {
            log.error("Failed to upload file to S3: bucket={}, key={}", bucket, key, e);
            throw new BusinessException(500, "文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            log.debug("File downloaded from S3: bucket={}, key={}", bucket, key);
            return response;
        } catch (Exception e) {
            log.error("Failed to download file from S3: bucket={}, key={}", bucket, key, e);
            throw new BusinessException(404, "文件不存在或下载失败: " + key);
        }
    }

    @Override
    public byte[] downloadBytes(String key) {
        try (InputStream is = download(key);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        } catch (IOException e) {
            log.error("Failed to read file content from S3: bucket={}, key={}", bucket, key, e);
            throw new BusinessException(500, "文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public String getPresignedUrl(String key, Duration expiration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(expiration != null ? expiration : Duration.ofSeconds(presignedUrlExpiration))
                .build();

            String url = presigner.presignGetObject(presignRequest).url().toString();
            log.debug("Generated presigned URL for S3 object: bucket={}, key={}", bucket, key);
            return url;
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: bucket={}, key={}", bucket, key, e);
            return null;
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            s3Client.headObject(request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

            s3Client.deleteObject(request);
            log.debug("File deleted from S3: bucket={}, key={}", bucket, key);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete file from S3: bucket={}, key={}", bucket, key, e);
            return false;
        }
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    /**
     * 关闭资源
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }
}