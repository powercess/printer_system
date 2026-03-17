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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "文件管理", description = "文件上传、查询和删除接口")
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
        Map<String, Object> result = fileService.upload(userId, file);
        return Result.success("上传成功", result);
    }

    @Operation(summary = "获取我的文件列表")
    @GetMapping("/list")
    public Result<PageResult<FileEntity>> getMyFiles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        PageResult<FileEntity> result = fileService.getMyFiles(userId, page, pageSize);
        return Result.success("获取成功", result);
    }

    @Operation(summary = "获取文件详情")
    @GetMapping("/detail")
    public Result<FileEntity> getFileDetail(@Parameter(description = "文件ID") @RequestParam Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        FileEntity file = fileService.getFileDetail(userId, fileId);
        return Result.success("获取成功", file);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public Result<Void> deleteFile(@Parameter(description = "文件ID") @RequestParam Long fileId) {
        Long userId = StpUtil.getLoginIdAsLong();
        fileService.deleteFile(userId, fileId);
        return Result.success("删除成功");
    }
}