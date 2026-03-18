package com.powercess.printer_system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.powercess.printer_system.dto.PageResult;
import com.powercess.printer_system.dto.order.OrderCreateRequest;
import com.powercess.printer_system.dto.order.PriceEstimateRequest;
import com.powercess.printer_system.entity.FileEntity;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.entity.OrderPromotion;
import com.powercess.printer_system.entity.Promotion;
import com.powercess.printer_system.exception.BusinessException;
import com.powercess.printer_system.mapper.FileMapper;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.mapper.OrderPromotionMapper;
import com.powercess.printer_system.mapper.PromotionMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务单元测试")
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private PromotionMapper promotionMapper;

    @Mock
    private OrderPromotionMapper orderPromotionMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("创建订单测试")
    class CreateOrderTests {

        @Test
        @DisplayName("应该成功创建订单 - 黑白单面")
        void shouldCreateOrder_BlackWhiteSimplex() {
            FileEntity file = new FileEntity();
            file.setId(1L);
            file.setPageCount(10);

            OrderCreateRequest request = new OrderCreateRequest(1L, "printer1", 0, 0, "A4", 1, null);

            when(fileMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(file));
            when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return 1;
            });

            Map<String, Object> result = orderService.createOrder(1L, request);

            assertThat(result).containsKeys("orderId", "originalAmount", "finalAmount");
            // 10 pages * 0.10 * 1 copy = 1.00
            assertThat((BigDecimal) result.get("originalAmount")).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat((BigDecimal) result.get("finalAmount")).isEqualByComparingTo(new BigDecimal("1.00"));

            verify(orderMapper).insert(any(Order.class));
        }

        @Test
        @DisplayName("应该成功创建订单 - 彩色双面")
        void shouldCreateOrder_ColorDuplex() {
            FileEntity file = new FileEntity();
            file.setId(1L);
            file.setPageCount(10);

            // colorMode=1 (彩色), duplex=1 (双面), copies=2
            OrderCreateRequest request = new OrderCreateRequest(1L, "printer1", 1, 1, "A4", 2, null);

            when(fileMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(file));
            when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return 1;
            });

            Map<String, Object> result = orderService.createOrder(1L, request);

            // 10 pages * 0.50 * 0.8 (双面折扣) * 2 copies = 8.00
            assertThat((BigDecimal) result.get("originalAmount")).isEqualByComparingTo(new BigDecimal("8.00"));
            assertThat((BigDecimal) result.get("finalAmount")).isEqualByComparingTo(new BigDecimal("8.00"));
        }

        @Test
        @DisplayName("文件不存在时应该抛出异常")
        void shouldThrowExceptionWhenFileNotFound() {
            OrderCreateRequest request = new OrderCreateRequest(999L, "printer1", 0, 0, "A4", 1, null);

            when(fileMapper.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件不存在")
                .extracting("code").isEqualTo(404);
        }

        @Test
        @DisplayName("使用有效优惠券应该计算折扣")
        void shouldCalculateDiscountWithValidPromotion() {
            FileEntity file = new FileEntity();
            file.setId(1L);
            file.setPageCount(10);

            Promotion promotion = new Promotion();
            promotion.setId(1L);
            promotion.setStatus(1);
            promotion.setDiscountType(0); // 固定金额
            promotion.setDiscountValue(new BigDecimal("2.00"));
            promotion.setStartTime(LocalDateTime.now().minusDays(1));
            promotion.setEndTime(LocalDateTime.now().plusDays(1));

            OrderCreateRequest request = new OrderCreateRequest(1L, "printer1", 0, 0, "A4", 1, 1L);

            when(fileMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(file));
            lenient().when(promotionMapper.selectById(1L)).thenReturn(promotion);
            lenient().when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(1L);
                return 1;
            });
            lenient().when(orderPromotionMapper.insert(any(OrderPromotion.class))).thenReturn(1);

            Map<String, Object> result = orderService.createOrder(1L, request);

            // original: 1.00, discount: 1.00 (min of 2.00 and 1.00)
            assertThat((BigDecimal) result.get("discountAmount")).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat((BigDecimal) result.get("finalAmount")).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("获取订单详情测试")
    class GetOrderDetailTests {

        @Test
        @DisplayName("应该成功获取订单详情")
        void shouldGetOrderDetail() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setFinalAmount(new BigDecimal("5.00"));

            when(orderMapper.selectById(1L)).thenReturn(order);

            Order result = orderService.getOrderDetail(1L, 1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("订单不存在时应该抛出异常")
        void shouldThrowExceptionWhenOrderNotFound() {
            when(orderMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单不存在")
                .extracting("code").isEqualTo(404);
        }

        @Test
        @DisplayName("非订单所有者访问时应该抛出异常")
        void shouldThrowExceptionWhenNotOwner() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(2L); // 属于用户2

            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 1L)) // 用户1尝试访问
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问此订单")
                .extracting("code").isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("取消订单测试")
    class CancelOrderTests {

        @Test
        @DisplayName("应该成功取消待处理的订单")
        void shouldCancelPendingOrder() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setStatus(0); // 待处理

            when(orderMapper.selectById(1L)).thenReturn(order);
            when(orderMapper.update(isNull(), any())).thenReturn(1);

            assertThatCode(() -> orderService.cancelOrder(1L, 1L)).doesNotThrowAnyException();

            verify(orderMapper).update(isNull(), any());
        }

        @Test
        @DisplayName("非订单所有者取消时应该抛出异常")
        void shouldThrowExceptionWhenNotOwnerCancel() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(2L);
            order.setStatus(0);

            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权取消此订单");
        }

        @Test
        @DisplayName("已完成订单取消时应该抛出异常")
        void shouldThrowExceptionWhenOrderCompleted() {
            Order order = new Order();
            order.setId(1L);
            order.setUserId(1L);
            order.setStatus(2); // 已完成

            when(orderMapper.selectById(1L)).thenReturn(order);

            assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单状态不允许取消");
        }
    }

    @Nested
    @DisplayName("价格估算测试")
    class EstimatePriceTests {

        @Test
        @DisplayName("应该正确估算价格")
        void shouldEstimatePrice() {
            FileEntity file = new FileEntity();
            file.setPageCount(20);

            PriceEstimateRequest request = new PriceEstimateRequest(1L, 0, 0, "A4", 1, null);

            when(fileMapper.findByIdNotDeleted(1L)).thenReturn(Optional.of(file));

            Map<String, BigDecimal> result = orderService.estimatePrice(1L, request);

            // 20 pages * 0.10 * 1 copy = 2.00
            assertThat(result.get("pageCount")).isEqualByComparingTo(new BigDecimal("20"));
            assertThat(result.get("originalAmount")).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(result.get("finalAmount")).isEqualByComparingTo(new BigDecimal("2.00"));
        }

        @Test
        @DisplayName("文件不存在时估算价格应该抛出异常")
        void shouldThrowExceptionWhenFileNotFoundForEstimate() {
            PriceEstimateRequest request = new PriceEstimateRequest(999L, 0, 0, "A4", 1, null);

            when(fileMapper.findByIdNotDeleted(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.estimatePrice(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("文件不存在");
        }
    }

    @Nested
    @DisplayName("获取订单列表测试")
    class GetMyOrdersTests {

        @Test
        @DisplayName("应该成功获取订单列表")
        void shouldGetMyOrders() {
            @SuppressWarnings("unchecked")
            IPage<Order> mockPage = mock(IPage.class);
            List<Order> orders = new ArrayList<>();
            Order order = new Order();
            order.setId(1L);
            orders.add(order);

            when(mockPage.getRecords()).thenReturn(orders);
            when(mockPage.getTotal()).thenReturn(1L);
            when(orderMapper.selectPage(any(), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

            PageResult<Order> result = orderService.getMyOrders(1L, 1, 10, null);

            assertThat(result.items()).hasSize(1);
            assertThat(result.total()).isEqualTo(1L);
        }
    }
}