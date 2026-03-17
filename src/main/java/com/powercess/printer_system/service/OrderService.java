package com.powercess.printer_system.service;

import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.order.OrderCreateRequest;
import com.powercess.printer_system.dto.order.PriceEstimateRequest;
import com.powercess.printer_system.entity.Order;

import java.math.BigDecimal;
import java.util.Map;

public interface OrderService {

    Map<String, Object> createOrder(Long userId, OrderCreateRequest request);

    Order getOrderDetail(Long userId, Long orderId);

    PageResult<Order> getMyOrders(Long userId, int page, int pageSize, Integer status);

    void cancelOrder(Long userId, Long orderId);

    Map<String, BigDecimal> estimatePrice(Long userId, PriceEstimateRequest request);
}