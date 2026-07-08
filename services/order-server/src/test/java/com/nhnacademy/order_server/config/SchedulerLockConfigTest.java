package com.nhnacademy.order_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;

class SchedulerLockConfigTest {

    @Test
    @DisplayName("스케줄러 설정은 스케줄링과 ShedLock을 활성화한다")
    void schedulerLockConfigEnablesSchedulingAndShedLock() {
        assertThat(SchedulerLockConfig.class).hasAnnotation(EnableScheduling.class);

        EnableSchedulerLock schedulerLock = SchedulerLockConfig.class.getAnnotation(EnableSchedulerLock.class);
        assertThat(schedulerLock).isNotNull();
        assertThat(schedulerLock.defaultLockAtMostFor()).isEqualTo("PT10M");
    }
}
