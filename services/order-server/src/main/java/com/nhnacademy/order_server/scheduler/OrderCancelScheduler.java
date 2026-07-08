package com.nhnacademy.order_server.scheduler;

import com.nhnacademy.order_server.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelScheduler {

    private final OrderService orderService;

    @Scheduled(cron = "0 0/10 * * * *")
    @SchedulerLock(name = "order.cancelExpiredOrders", lockAtMostFor = "PT9M", lockAtLeastFor = "PT30S")
    public void runOrderAutoCancel() {
        log.info("[Scheduler] payment-waiting order expiration cleanup started");
        orderService.cancelExpiredOrders();
        log.info("[Scheduler] payment-waiting order expiration cleanup completed");
    }

    @Scheduled(cron = "0 0 3 * * *")
    @SchedulerLock(name = "order.dailyStatusUpdate", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void runDailyOrderStatusUpdate() {
        log.info("[Scheduler] daily order status update started");
        orderService.autoCompleteDelivery();
        orderService.autoConfirmPurchase();
        log.info("[Scheduler] daily order status update completed");
    }
}
