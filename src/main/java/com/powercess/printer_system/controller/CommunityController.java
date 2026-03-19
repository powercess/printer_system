package com.powercess.printer_system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.community.ShareCreateRequest;
import com.powercess.printer_system.entity.CommunityShare;
import com.powercess.printer_system.entity.FileEntity;
import com.powercess.printer_system.entity.Like;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.CommunityShareMapper;
import com.powercess.printer_system.mapper.FileMapper;
import com.powercess.printer_system.mapper.LikeMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "社区分享", description = "文件分享、点赞和社区广场接口")
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityShareMapper communityShareMapper;
    private final FileMapper fileMapper;
    private final LikeMapper likeMapper;

    @Operation(summary = "发布分享")
    @PostMapping("/share")
    public Result<Void> createShare(@Valid @RequestBody ShareCreateRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Creating share: fileId={}", userId, request.fileId());

        FileEntity file = fileMapper.findByIdNotDeleted(request.fileId())
            .orElseThrow(() -> new BusinessException(404, "文件不存在"));

        if (!file.getUserId().equals(userId)) {
            log.warn("[{}] Share denied - no permission: fileId={}", userId, request.fileId());
            throw new BusinessException(403, "无权分享此文件");
        }

        LambdaQueryWrapper<CommunityShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityShare::getUserId, userId)
            .eq(CommunityShare::getFileId, request.fileId())
            .isNull(CommunityShare::getDeletedAt);
        if (communityShareMapper.selectCount(wrapper) > 0) {
            log.warn("[{}] Share already exists: fileId={}", userId, request.fileId());
            throw new BusinessException(400, "该文件已经分享过");
        }

        CommunityShare share = new CommunityShare();
        share.setUserId(userId);
        share.setFileId(request.fileId());
        share.setCreatedAt(LocalDateTime.now());
        communityShareMapper.insert(share);

        log.info("[{}] Share created: shareId={}, fileId={}", userId, share.getId(), request.fileId());
        return Result.success("分享成功");
    }

    @Operation(summary = "获取分享列表")
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getShares(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("[{}] Getting shares list: page={}, pageSize={}", userId, page, pageSize);

        List<CommunityShare> shares = communityShareMapper.findAllWithDetails(userId);

        List<Map<String, Object>> items = shares.stream()
            .skip((long) (page - 1) * pageSize)
            .limit(pageSize)
            .map(s -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", s.getId());
                map.put("userId", s.getUserId());
                map.put("username", s.getUsername());
                map.put("nickname", s.getNickname());
                map.put("fileId", s.getFileId());
                map.put("fileName", s.getFileName());
                map.put("filePath", s.getFilePath());
                map.put("createdAt", s.getCreatedAt());
                map.put("likeCount", s.getLikeCount());
                map.put("isLiked", s.getIsLiked());
                return map;
            })
            .toList();

        return Result.success("获取成功", PageResult.of(shares.size(), page, pageSize, items));
    }

    @Operation(summary = "点赞")
    @PostMapping("/like")
    public Result<Void> likeShare(@Parameter(description = "分享ID") @RequestParam Long shareId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Liking share: shareId={}", userId, shareId);

        CommunityShare share = communityShareMapper.selectById(shareId);
        if (share == null) {
            log.warn("[{}] Like failed - share not found: shareId={}", userId, shareId);
            throw new BusinessException(404, "分享不存在");
        }

        if (likeMapper.countByShareIdAndUserId(shareId, userId) > 0) {
            log.warn("[{}] Like failed - already liked: shareId={}", userId, shareId);
            throw new BusinessException(400, "已经点赞过");
        }

        Like like = new Like();
        like.setShareId(shareId);
        like.setUserId(userId);
        like.setCreatedAt(LocalDateTime.now());
        likeMapper.insert(like);

        log.info("[{}] Like success: shareId={}", userId, shareId);
        return Result.success("点赞成功");
    }

    @Operation(summary = "取消点赞")
    @DeleteMapping("/unlike")
    public Result<Void> unlikeShare(@Parameter(description = "分享ID") @RequestParam Long shareId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Unliking share: shareId={}", userId, shareId);

        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getShareId, shareId).eq(Like::getUserId, userId);
        Like like = likeMapper.selectOne(wrapper);

        if (like == null) {
            log.warn("[{}] Unlike failed - not liked: shareId={}", userId, shareId);
            throw new BusinessException(400, "未点赞过");
        }

        likeMapper.deleteById(like.getId());
        log.info("[{}] Unlike success: shareId={}", userId, shareId);
        return Result.success("取消点赞成功");
    }

    @Operation(summary = "删除分享")
    @DeleteMapping("/delete")
    public Result<Void> deleteShare(@Parameter(description = "分享ID") @RequestParam Long shareId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("[{}] Deleting share: shareId={}", userId, shareId);

        CommunityShare share = communityShareMapper.selectById(shareId);
        if (share == null) {
            log.warn("[{}] Delete share failed - not found: shareId={}", userId, shareId);
            throw new BusinessException(400, "分享不存在");
        }

        if (!share.getUserId().equals(userId)) {
            log.warn("[{}] Delete share denied - no permission: shareId={}", userId, shareId);
            throw new BusinessException(403, "无权删除此分享");
        }

        share.setDeletedAt(LocalDateTime.now());
        communityShareMapper.updateById(share);

        log.info("[{}] Share deleted: shareId={}", userId, shareId);
        return Result.success("删除成功");
    }
}