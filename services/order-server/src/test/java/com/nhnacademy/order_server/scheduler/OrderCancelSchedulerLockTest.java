package com.nhnacademy.order_server.scheduler;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class OrderCancelSchedulerLockTest {

    @Test
    @DisplayName("결제 대기 만료 주문 정리 스케줄러는 분산 락을 사용한다")
    void runOrderAutoCancelUsesDistributedLock() throws NoSuchMethodException {
        Method method = OrderCancelScheduler.class.getMethod("runOrderAutoCancel");

        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.name()).isEqualTo("order.cancelExpiredOrders");
        assertThat(lock.lockAtMostFor()).isEqualTo("PT9M");
        assertThat(lock.lockAtLeastFor()).isEqualTo("PT30S");
    }

    @Test
    @DisplayName("일일 주문 상태 변경 스케줄러는 분산 락을 사용한다")
    void runDailyOrderStatusUpdateUsesDistributedLock() throws NoSuchMethodException {
        Method method = OrderCancelScheduler.class.getMethod("runDailyOrderStatusUpdate");

        SchedulerLock lock = method.getAnnotation(SchedulerLock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.name()).isEqualTo("order.dailyStatusUpdate");
        assertThat(lock.lockAtMostFor()).isEqualTo("PT30M");
        assertThat(lock.lockAtLeastFor()).isEqualTo("PT1M");
    }
}
