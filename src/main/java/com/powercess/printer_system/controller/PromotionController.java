package com.powercess.printer_system.controller;

import com.powercess.printer_system.dto.Result;
import com.powercess.printer_system.dto.promotion.PromotionValidateRequest;
import com.powercess.printer_system.entity.Promotion;
import com.powercess.printer_system.mapper.PromotionMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "促销活动", description = "促销活动查询和验证接口")
@RestController
@RequestMapping("/api/promotion")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionMapper promotionMapper;

    @Operation(summary = "获取有效促销活动列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();

        List<Promotion> promotions = promotionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Promotion>()
                .eq(Promotion::getStatus, 1)
                .le(Promotion::getStartTime, now)
                .ge(Promotion::getEndTime, now)
                .orderByDesc(Promotion::getId)
        );

        List<Map<String, Object>> items = promotions.stream()
            .map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("title", p.getTitle());
                map.put("discountType", p.getDiscountType());
                map.put("discountValue", p.getDiscountValue());
                map.put("startTime", p.getStartTime());
                map.put("endTime", p.getEndTime());
                map.put("status", p.getStatus());
                return map;
            })
            .toList();

        return Result.success("获取成功", Map.of("items", items));
    }

    @Operation(summary = "验证促销活动")
    @PostMapping("/validate")
    public Result<Map<String, Object>> validatePromotion(@RequestBody PromotionValidateRequest request) {

        Promotion promotion = promotionMapper.selectById(request.promotionId());
        if (promotion == null) {
            return Result.success("验证成功", Map.of(
                "valid", false,
                "discountAmount", 0.0,
                "message", "促销活动不存在"
            ));
        }

        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStatus() != 1) {
            return Result.success("验证成功", Map.of(
                "valid", false,
                "discountAmount", 0.0,
                "message", "促销活动已暂停"
            ));
        }

        if (promotion.getStartTime().isAfter(now)) {
            return Result.success("验证成功", Map.of(
                "valid", false,
                "discountAmount", 0.0,
                "message", "促销活动未开始"
            ));
        }

        if (promotion.getEndTime().isBefore(now)) {
            return Result.success("验证成功", Map.of(
                "valid", false,
                "discountAmount", 0.0,
                "message", "促销活动已结束"
            ));
        }

        return Result.success("验证成功", Map.of(
            "valid", true,
            "discountAmount", promotion.getDiscountValue(),
            "message", "促销活动可用"
        ));
    }
}