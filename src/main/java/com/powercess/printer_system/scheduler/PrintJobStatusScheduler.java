package com.powercess.printer_system.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.powercess.printer_system.entity.Order;
import com.powercess.printer_system.mapper.OrderMapper;
import com.powercess.printer_system.service.CupsClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cups4j.PrintJobAttributes;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task to poll CUPS print job status and update order status accordingly.
 *
 * Order status codes:
 * 0 - pending
 * 1 - paid
 * 2 - printing
 * 3 - completed
 * 4 - cancelled
 * 5 - failed
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrintJobStatusScheduler {

    private final OrderMapper orderMapper;
    private final CupsClientService cupsClientService;

    /**
     * Poll CUPS job status every 10 seconds for orders in "printing" state.
     */
    @Scheduled(fixedRate = 10000)
    public void checkPrintJobStatus() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 2)  // printing status
               .isNotNull(Order::getCupsJobId);

        List<Order> printingOrders = orderMapper.selectList(wrapper);

        if (printingOrders.isEmpty()) {
            return;
        }

        log.debug("Checking status for {} printing orders", printingOrders.size());

        for (Order order : printingOrders) {
            try {
                checkAndUpdateOrderStatus(order);
            } catch (Exception e) {
                log.error("Failed to check job status for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    private void checkAndUpdateOrderStatus(Order order) {
        Integer jobId = order.getCupsJobId();
        if (jobId == null) {
            return;
        }

        try {
            PrintJobAttributes jobAttributes = cupsClientService.getJobAttributes(jobId);
            if (jobAttributes == null || jobAttributes.getJobState() == null) {
                return;
            }

            String jobState = jobAttributes.getJobState().name();
            log.debug("Order {} job {} state: {}", order.getId(), jobId, jobState);

            Integer newStatus = mapJobStateToOrderStatus(jobState);
            if (newStatus != null && !newStatus.equals(order.getStatus())) {
                order.setStatus(newStatus);
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
                log.info("Order {} status updated from 2 to {} (job state: {})",
                    order.getId(), newStatus, jobState);
            }
        } catch (Exception e) {
            // Job might not exist in CUPS anymore (already completed and purged)
            // If the job is not found, we can assume it completed
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                log.info("Job {} not found in CUPS, assuming completed for order {}", jobId, order.getId());
                order.setStatus(3);  // completed
                order.setUpdatedAt(LocalDateTime.now());
                orderMapper.updateById(order);
            } else {
                log.warn("Error checking job status for order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    /**
     * Map CUPS job state to order status.
     *
     * CUPS job states:
     * - PENDING: Job is waiting to be processed
     * - PROCESSING: Job is currently printing
     * - COMPLETED: Job completed successfully
     * - ABORTED: Job was aborted
     * - CANCELED: Job was cancelled
     * - STOPPED: Job stopped due to error
     *
     * @param jobState CUPS job state name
     * @return Order status code, or null if no update needed
     */
    private Integer mapJobStateToOrderStatus(String jobState) {
        return switch (jobState) {
            case "COMPLETED" -> 3;  // completed
            case "ABORTED", "STOPPED" -> 5;  // failed
            case "CANCELED" -> 4;  // cancelled
            case "PENDING", "PROCESSING", "HELD" -> null;  // still printing, no update
            default -> null;
        };
    }
}