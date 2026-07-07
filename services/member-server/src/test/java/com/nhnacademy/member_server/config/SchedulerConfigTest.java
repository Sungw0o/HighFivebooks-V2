package com.nhnacademy.member_server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class SchedulerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SchedulerConfig.class)
            // SchedulerConfig는 RedisConnectionFactory를 필요로 하므로 Mock 빈 주입
            .withBean(RedisConnectionFactory.class, () -> mock(RedisConnectionFactory.class));

    @Test
    @DisplayName("SchedulerConfig 빈 등록 확인")
    void schedulerConfigLoads() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LockProvider.class);
            // @EnableScheduling 애노테이션 확인은 빈 존재 여부로 간접 확인
        });
    }
}