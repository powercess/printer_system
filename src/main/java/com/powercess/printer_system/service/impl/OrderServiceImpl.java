package com.powercess.printer_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.order.OrderCreateRequest;
import com.powercess.printer_system.dto.order.PriceEstimateRequest;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.OrderPromotion;
import com.powercess.printer_system.entity.Promotion;
import com.powercess.printer_system.entity.UserFile;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.mapper.OrderPromotionMapper;
import com.powercess.printer_system.mapper.PromotionMapper;
import com.powercess.printer_system.mapper.UserFileMapper;
import com.powercess.printer_system.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final UserFileMapper userFileMapper;
    private final PromotionMapper promotionMapper;
    private final OrderPromotionMapper orderPromotionMapper;

    @Override
    @Transactional
    public Map<String, Object> createOrder(Long userId, OrderCreateRequest request) {
        log.debug("Creating order for user: userId={}, fileId={}", userId, request.fileId());

        UserFile file = userFileMapper.findByIdNotDeletedWithBlob(request.fileId())
            .orElseThrow(() -> {
                log.warn("File not found for order: fileId={}", request.fileId());
                return new BusinessException(404, "文件不存在");
            });

        Map<String, BigDecimal> priceInfo = calculatePrice(file.getPageCount(), request.colorMode(),
            request.duplex(), request.copies(), request.promotionId(), userId);

        Order order = new Order();
        order.setUserId(userId);
        order.setFileId(request.fileId());
        order.setPrinterName(request.printerName());
        order.setOriginalAmount(priceInfo.get("originalAmount"));
        order.setDiscountAmount(priceInfo.get("discountAmount"));
        order.setFinalAmount(priceInfo.get("finalAmount"));
        order.setColorMode(request.colorMode());
        order.setDuplex(request.duplex());
        order.setPaperSize(request.paperSize());
        order.setCopies(request.copies());
        order.setStatus(0);
        order.setCreatedAt(LocalDateTime.now());

        orderMapper.insert(order);
        log.info("Order created: orderId={}, userId={}, finalAmount={}", order.getId(), userId, order.getFinalAmount());

        if (request.promotionId() != null) {
            OrderPromotion orderPromotion = new OrderPromotion();
            orderPromotion.setOrderId(order.getId());
            orderPromotion.setPromotionId(request.promotionId());
            orderPromotion.setCreatedAt(LocalDateTime.now());
            orderPromotionMapper.insert(orderPromotion);
            log.debug("Promotion applied to order: orderId={}, promotionId={}", order.getId(), request.promotionId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("printerName", request.printerName());
        result.put("originalAmount", priceInfo.get("originalAmount"));
        result.put("discountAmount", priceInfo.get("discountAmount"));
        result.put("finalAmount", priceInfo.get("finalAmount"));
        result.put("status", 0);

        return result;
    }

    @Override
    public Order getOrderDetail(Long userId, Long orderId) {
        log.debug("Getting order detail: userId={}, orderId={}", userId, orderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("Order not found: orderId={}", orderId);
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            log.warn("Order access denied: userId={}, orderId={}, ownerUserId={}", userId, orderId, order.getUserId());
            throw new BusinessException(403, "无权访问此订单");
        }
        return order;
    }

    @Override
    public PageResult<Order> getMyOrders(Long userId, int page, int pageSize, Integer status) {
        log.debug("Getting user orders: userId={}, page={}, pageSize={}, status={}", userId, page, pageSize, status);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        IPage<Order> pageResult = orderMapper.selectPage(
            new Page<>(page, Math.min(pageSize, 100)), wrapper);

        return PageResult.of(pageResult.getTotal(), page, pageSize, pageResult.getRecords());
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        log.info("Cancelling order: userId={}, orderId={}", userId, orderId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            log.warn("Order not found for cancellation: orderId={}", orderId);
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            log.warn("Order cancellation denied: userId={}, orderId={}, ownerUserId={}", userId, orderId, order.getUserId());
            throw new BusinessException(403, "无权取消此订单");
        }
        if (order.getStatus() != 0 && order.getStatus() != 4) {
            log.warn("Order cannot be cancelled: orderId={}, status={}", orderId, order.getStatus());
            throw new BusinessException(400, "订单状态不允许取消");
        }

        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Order::getId, orderId)
            .set(Order::getStatus, 4)
            .set(Order::getUpdatedAt, LocalDateTime.now());
        orderMapper.update(null, updateWrapper);
        log.info("Order cancelled successfully: orderId={}", orderId);
    }

    @Override
    public Map<String, BigDecimal> estimatePrice(Long userId, PriceEstimateRequest request) {
        log.debug("Estimating price: userId={}, fileId={}", userId, request.fileId());
        UserFile file = userFileMapper.findByIdNotDeletedWithBlob(request.fileId())
            .orElseThrow(() -> {
                log.warn("File not found for price estimation: fileId={}", request.fileId());
                return new BusinessException(404, "文件不存在");
            });

        return calculatePrice(file.getPageCount(), request.colorMode(),
            request.duplex(), request.copies(), request.promotionId(), userId);
    }

    private Map<String, BigDecimal> calculatePrice(int pageCount, int colorMode, int duplex,
                                                    int copies, Long promotionId, Long userId) {
        log.debug("Calculating price: pageCount={}, colorMode={}, duplex={}, copies={}, promotionId={}",
            pageCount, colorMode, duplex, copies, promotionId);

        // 计算实际纸张张数
        // 单面打印（duplex=0）：1页 = 1张
        // 双面打印（duplex=1）：2页 = 1张（向上取整）
        int sheetCount;
        if (duplex == 1) {
            // 双面打印：每张纸可以打印2页
            sheetCount = (pageCount + 1) / 2; // 等同于 Math.ceil(pageCount / 2.0)
            log.debug("Duplex printing: {} pages = {} sheets", pageCount, sheetCount);
        } else {
            // 单面打印：1页 = 1张
            sheetCount = pageCount;
            log.debug("Simplex printing: {} pages = {} sheets", pageCount, sheetCount);
        }

        // 统一价格：0.2 元/张
        BigDecimal basePrice = new BigDecimal("0.20");

        BigDecimal originalAmount = basePrice.multiply(BigDecimal.valueOf(sheetCount))
            .multiply(BigDecimal.valueOf(copies))
            .setScale(2, RoundingMode.HALF_UP);

        log.debug("Price calculation: {} sheets × {} yuan × {} copies = {} yuan",
            sheetCount, basePrice, copies, originalAmount);

        BigDecimal discountAmount = BigDecimal.ZERO;

        if (promotionId != null) {
            Promotion promotion = promotionMapper.selectById(promotionId);
            if (promotion != null && promotion.getStatus() == 1) {
                LocalDateTime now = LocalDateTime.now();
                if (!promotion.getStartTime().isAfter(now) && !promotion.getEndTime().isBefore(now)) {
                    discountAmount = calculateDiscount(promotion, originalAmount, userId);
                    log.debug("Promotion discount applied: promotionId={}, discountAmount={}", promotionId, discountAmount);
                } else {
                    log.debug("Promotion not in valid time range: promotionId={}", promotionId);
                }
            } else {
                log.debug("Promotion not valid: promotionId={}", promotionId);
            }
        }

        BigDecimal finalAmount = originalAmount.subtract(discountAmount)
            .setScale(2, RoundingMode.HALF_UP);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("pageCount", BigDecimal.valueOf(pageCount)); // PDF页数
        result.put("sheetCount", BigDecimal.valueOf(sheetCount)); // 实际张数
        result.put("originalAmount", originalAmount);
        result.put("discountAmount", discountAmount);
        result.put("finalAmount", finalAmount);

        log.info("Price calculated: pageCount={}, sheetCount={}, originalAmount={}, discountAmount={}, finalAmount={}",
            pageCount, sheetCount, originalAmount, discountAmount, finalAmount);
        return result;
    }

    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal originalAmount, Long userId) {
        return switch (promotion.getDiscountType()) {
            case 0 -> promotion.getDiscountValue().min(originalAmount);
            case 1 -> originalAmount.multiply(promotion.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case 2 -> {
                LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Order::getUserId, userId).gt(Order::getStatus, 0);
                long count = orderMapper.selectCount(wrapper);
                yield count == 0 ? originalAmount : BigDecimal.ZERO;
            }
            default -> BigDecimal.ZERO;
        };
    }
}