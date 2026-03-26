package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.file.PreviewSession;
import com.powercess.printer_system.entity.UserFile;
import com.powercess.printer_system.service.FileService;
import com.powercess.printer_system.service.PreviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件管理", description = "文件上传、预览、查询和删除接口")
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final PreviewSessionService previewSessionService;

    // ==================== 预览相关接口 ====================

    @Operation(summary = "上传文件预览", description = "上传文件并转换为PDF进行预览，返回预览会话ID")
    @PostMapping("/preview/upload")
    public Result<Map<String, Object>> uploadForPreview(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();
        log.info("[{}] Uploading file for preview: name={}, size={}bytes", userId, originalFilename, fileSize);

        PreviewSession session = previewSessionService.createPreview(userId, file);

        log.info("[{}] Preview session created: sessionId={}, converted={}", userId, session.getSessionId(), session.isConverted());

        Map<String, Object> result = Map.of(
            "sessionId", session.getSessionId(),
            "originalFilename", session.getOriginalFilename(),
            "originalFileType", session.getOriginalFileType(),
            "originalFileSize", session.getOriginalFileSize(),
            "pdfSize", session.getPdfSize(),
            "converted", session.isConverted(),
            "pageCount", session.getPageCount(),
            "expiresAt", session.getExpiresAt().toString()
        );

        return Result.success("上传成功，请预览后确认", result);
    }

    @Operation(summary = "获取预览PDF", description = "获取预览会话对应的PDF文件内容")
    @GetMapping("/preview/pdf/{sessionId}")
    public ResponseEntity<byte[]> getPreviewPdf(
            @Parameter(description = "预览会话ID") @PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting preview PDF: sessionId={}", userId, sessionId);

        byte[] pdfData = previewSessionService.getPdfData(sessionId, userId);
        PreviewSession session = previewSessionService.getPreview(sessionId, userId);

        String filename = session.getOriginalFilename();
        if (!filename.toLowerCase().endsWith(".pdf")) {
            int lastDot = filename.lastIndexOf(".");
            if (lastDot > 0) {
                filename = filename.substring(0, lastDot) + ".pdf";
            } else {
                filename = filename + ".pdf";
            }
        }

        log.info("[{}] Returning preview PDF: sessionId={}, size={}bytes", userId, sessionId, pdfData.length);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdfData);
    }

    @Operation(summary = "获取预览会话状态")
    @GetMapping("/preview/status")
    public Result<Map<String, Object>> getPreviewStatus(
            @Parameter(description = "预览会话ID") @RequestParam String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting preview status: sessionId={}", userId, sessionId);

        PreviewSession session = previewSessionService.getPreview(sessionId, userId);

        Map<String, Object> result = Map.of(
            "sessionId", session.getSessionId(),
            "status", session.getStatus(),
            "originalFilename", session.getOriginalFilename(),
            "converted", session.isConverted(),
            "pageCount", session.getPageCount(),
            "expiresAt", session.getExpiresAt().toString()
        );

        return Result.success("获取成功", result);
    }

    @Operation(summary = "确认预览并保存文件", description = "用户确认预览后，将PDF文件正式保存")
    @PostMapping("/preview/confirm")
    public Result<Map<String, Object>> confirmPreview(
            @Parameter(description = "预览会话ID") @RequestParam String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Confirming preview: sessionId={}", userId, sessionId);

        Long fileId = previewSessionService.confirmPreview(sessionId, userId);

        log.info("[{}] Preview confirmed, file saved: fileId={}", userId, fileId);

        Map<String, Object> result = Map.of(
            "fileId", fileId,
            "message", "文件已保存"
        );

        return Result.success("保存成功", result);
    }

    @Operation(summary = "取消预览")
    @DeleteMapping("/preview/cancel")
    public Result<Void> cancelPreview(
            @Parameter(description = "预览会话ID") @RequestParam String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Cancelling preview: sessionId={}", userId, sessionId);

        previewSessionService.cancelPreview(sessionId, userId);

        return Result.success("已取消");
    }

    // ==================== 原有接口 ====================

    @Operation(summary = "上传文件（直接保存，不预览）", description = "直接上传PDF文件并保存，不支持转换")
    @PostMapping("/upload")
    public Result<Map<String, Object>> uploadFile(
            @Parameter(description = "文件") @RequestParam("file") MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        String originalFilename = file.getOriginalFilename();
        long fileSize = file.getSize();
        log.info("[{}] Uploading file: name={}, size={}bytes", userId, originalFilename, fileSize);
        Map<String, Object> result = fileService.upload(userId, file);
        log.info("[{}] File uploaded: fileId={}, pageCount={}", userId, result.get("fileId"), result.get("pageCount"));
        return Result.success("上传成功", result);
    }

    @Operation(summary = "获取我的文件列表")
    @GetMapping("/list")
    public Result<PageResult<UserFile>> getMyFiles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting file list: page={}, pageSize={}", userId, page, pageSize);
        PageResult<UserFile> result = fileService.getMyFiles(userId, page, pageSize);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取文件详情")
    @GetMapping("/detail")
    public Result<UserFile> getFileDetail(@Parameter(description = "文件ID") @RequestParam Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting file detail: fileId={}", userId, fileId);
        UserFile file = fileService.getFileDetail(userId, fileId);
        return Result.success("获取成功", file);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@Parameter(description = "文件ID") @RequestParam Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Deleting file: fileId={}", userId, fileId);
        fileService.deleteFile(userId, fileId);
        log.info("[{}] File deleted: fileId={}", userId, fileId);
        return Result.success("删除成功");
    }

    @Operation(summary = "获取文件下载链接")
    @GetMapping("/download-url")
    public Result<Map<String, String>> getDownloadUrl(@Parameter(description = "文件ID") @RequestParam Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting download URL: fileId={}", userId, fileId);
        String downloadUrl = fileService.getDownloadUrl(userId, fileId);
        Map<String, String> result = Map.of(
            "fileId", String.valueOf(fileId),
            "downloadUrl", downloadUrl
        );
        return Result.success("获取成功", result);
    }
}