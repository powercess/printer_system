package com.powercess.printer_system.service.impl;

import com.powercess.printer_system.dto.file.PreviewSession;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileBlobMapper;
import com.powercess.printer_system.mapper.UserFileMapper;
import com.powercess.printer_system.service.PdfConversionService;
import com.powercess.printer_system.service.PreviewSessionService;
import com.powercess.printer_system.service.StorageService;
import com.powercess.printer_system.entity.FileBlob;
import com.powercess.printer_system.entity.UserFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 预览会话服务实现
 * 使用内存存储临时预览数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewSessionServiceImpl implements PreviewSessionService {

    private final PdfConversionService pdfConversionService;
    private final StorageService storageService;
    private final UserFileMapper userFileMapper;
    private final FileBlobMapper fileBlobMapper;

    // 内存存储预览会话（生产环境应使用 Redis）
    private final Map<String, PreviewSession> sessions = new ConcurrentHashMap<>();

    // 预览会话过期时间（分钟）
    private static final int EXPIRATION_MINUTES = 30;

    @Override
    public PreviewSession createPreview(Long userId, MultipartFile file) {
        log.info("Creating preview session for user: userId={}, filename={}", userId, file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "unknown_file";
        }

        String originalFileType = pdfConversionService.getFileType(originalFilename);
        long originalFileSize = file.getSize();

        // 检查文件类型
        boolean isPdf = pdfConversionService.isPdf(originalFilename);
        boolean needsConversion = !isPdf && pdfConversionService.isConvertible(originalFilename);

        if (!isPdf && !needsConversion) {
            throw new BusinessException(400, "不支持的文件类型，请上传 PDF 或 Office 文档");
        }

        // 转换为 PDF
        byte[] pdfData;
        try (var inputStream = pdfConversionService.convertToPdf(file)) {
            pdfData = inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("Failed to convert file to PDF: {}", originalFilename, e);
            throw new BusinessException(500, "文件转换失败: " + e.getMessage());
        }

        // 创建预览会话
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRATION_MINUTES);

        PreviewSession session = PreviewSession.builder()
            .sessionId(sessionId)
            .userId(userId)
            .originalFilename(originalFilename)
            .originalFileType(originalFileType)
            .originalFileSize(originalFileSize)
            .pdfData(pdfData)
            .pdfSize((long) pdfData.length)
            .converted(needsConversion)
            .createdAt(now)
            .expiresAt(expiresAt)
            .status("pending")
            .pageCount(1) // TODO: 计算实际页数
            .build();

        sessions.put(sessionId, session);
        log.info("Preview session created: sessionId={}, userId={}, pdfSize={}", sessionId, userId, pdfData.length);

        return session;
    }

    @Override
    public PreviewSession getPreview(String sessionId, Long userId) {
        PreviewSession session = sessions.get(sessionId);
        if (session == null) {
            throw new BusinessException(404, "预览会话不存在或已过期");
        }

        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问此预览");
        }

        if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
            sessions.remove(sessionId);
            throw new BusinessException(410, "预览会话已过期");
        }

        return session;
    }

    @Override
    public Long confirmPreview(String sessionId, Long userId) {
        log.info("Confirming preview: sessionId={}, userId={}", sessionId, userId);

        PreviewSession session = getPreview(sessionId, userId);

        if (!"pending".equals(session.getStatus())) {
            throw new BusinessException(400, "预览会话状态无效");
        }

        try {
            // 计算 PDF 内容哈希
            String contentHash = calculateSHA256(session.getPdfData());

            // 检查是否已存在相同内容的文件
            FileBlob existingBlob = fileBlobMapper.findByContentHash(contentHash).orElse(null);

            FileBlob blob;
            if (existingBlob != null && existingBlob.getStoragePath() != null) {
                // 检查物理文件是否存在
                if (storageService.exists(existingBlob.getStoragePath())) {
                    blob = existingBlob;
                    log.info("Reusing existing blob: blobId={}", blob.getId());
                } else {
                    // 物理文件不存在，需要重新存储
                    blob = storePdf(session.getPdfData(), contentHash);
                }
            } else {
                // 新文件，存储
                blob = storePdf(session.getPdfData(), contentHash);
            }

            // 创建 UserFile 记录
            // 生成 PDF 文件名（将原始文件名转换为 .pdf）
            String pdfFilename = session.getOriginalFilename();
            if (!pdfFilename.toLowerCase().endsWith(".pdf")) {
                int lastDot = pdfFilename.lastIndexOf(".");
                if (lastDot > 0) {
                    pdfFilename = pdfFilename.substring(0, lastDot) + ".pdf";
                } else {
                    pdfFilename = pdfFilename + ".pdf";
                }
            }

            UserFile userFile = new UserFile();
            userFile.setUserId(userId);
            userFile.setBlobId(blob.getId());
            userFile.setDisplayName(pdfFilename);
            userFile.setPageCount(session.getPageCount());
            userFile.setUploadTime(LocalDateTime.now());
            userFile.setCreatedAt(LocalDateTime.now());

            userFileMapper.insert(userFile);

            // 增加引用计数
            fileBlobMapper.incrementRefCount(blob.getId());

            // 更新会话状态并移除
            session.setStatus("confirmed");
            sessions.remove(sessionId);

            log.info("Preview confirmed, file saved: fileId={}, userId={}", userFile.getId(), userId);
            return userFile.getId();

        } catch (Exception e) {
            log.error("Failed to confirm preview: sessionId={}", sessionId, e);
            throw new BusinessException(500, "文件保存失败: " + e.getMessage());
        }
    }

    @Override
    public void cancelPreview(String sessionId, Long userId) {
        log.info("Cancelling preview: sessionId={}, userId={}", sessionId, userId);

        PreviewSession session = getPreview(sessionId, userId);
        session.setStatus("cancelled");
        sessions.remove(sessionId);

        log.info("Preview cancelled: sessionId={}", sessionId);
    }

    @Override
    public byte[] getPdfData(String sessionId, Long userId) {
        PreviewSession session = getPreview(sessionId, userId);
        return session.getPdfData();
    }

    @Override
    @Scheduled(fixedRate = 300000) // 每5分钟清理一次
    public void cleanupExpiredSessions() {
        log.debug("Cleaning up expired preview sessions");
        LocalDateTime now = LocalDateTime.now();
        int removedCount = 0;

        for (Map.Entry<String, PreviewSession> entry : sessions.entrySet()) {
            if (now.isAfter(entry.getValue().getExpiresAt())) {
                sessions.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Cleaned up {} expired preview sessions", removedCount);
        }
    }

    /**
     * 存储 PDF 文件
     */
    private FileBlob storePdf(byte[] pdfData, String contentHash) {
        // 构建存储路径
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String hashedFilename = contentHash.substring(0, 16) + ".pdf";
        String relativePath = datePath + "/" + hashedFilename;

        // 上传到存储
        String storedPath = storageService.upload(
            relativePath,
            new ByteArrayInputStream(pdfData),
            (long) pdfData.length,
            "application/pdf"
        );

        // 创建 FileBlob 记录
        FileBlob blob = new FileBlob();
        blob.setContentHash(contentHash);
        blob.setStoragePath(storedPath);
        blob.setFileSize((long) pdfData.length);
        blob.setFileType("pdf");
        blob.setRefCount(0);
        blob.setCreatedAt(LocalDateTime.now());

        fileBlobMapper.insert(blob);
        log.info("New blob created: blobId={}", blob.getId());

        return blob;
    }

    /**
     * 计算 SHA-256 哈希值
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
}