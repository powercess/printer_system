package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.entity.FileEntity;
import com.powercess.printer_system.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件管理", description = "文件上传、查询和删除接口")
@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件")
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
    public Result<PageResult<FileEntity>> getMyFiles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting file list: page={}, pageSize={}", userId, page, pageSize);
        PageResult<FileEntity> result = fileService.getMyFiles(userId, page, pageSize);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取文件详情")
    @GetMapping("/detail")
    public Result<FileEntity> getFileDetail(@Parameter(description = "文件ID") @RequestParam Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting file detail: fileId={}", userId, fileId);
        FileEntity file = fileService.getFileDetail(userId, fileId);
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