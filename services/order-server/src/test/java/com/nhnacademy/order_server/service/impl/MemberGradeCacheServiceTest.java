package com.nhnacademy.order_server.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.order_server.adapter.MemberClient;
import com.nhnacademy.order_server.config.MemberGradeCacheConfig;
import com.nhnacademy.order_server.dto.response.external.MemberGradeResponse;
import com.nhnacademy.order_server.observability.OrderCreateMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {
        MemberGradeCacheConfig.class,
        MemberGradeCacheService.class,
        MemberGradeCacheServiceTest.CacheTestConfig.class
})
@TestPropertySource(properties = {
        "order.member-grade-cache.type=caffeine",
        "order.member-grade-cache.ttl=5m",
        "order.member-grade-cache.maximum-size=100"
})
class MemberGradeCacheServiceTest {

    @Autowired
    private MemberGradeCacheService memberGradeCacheService;

    @MockitoBean
    private MemberClient memberClient;

    @BeforeEach
    void clearCache(@Autowired org.springframework.cache.CacheManager memberGradeCacheManager) {
        memberGradeCacheManager.getCache(MemberGradeCacheConfig.CACHE_NAME).clear();
    }

    @Test
    @DisplayName("같은 회원의 적립률은 로컬 캐시에서 반환한다")
    void cachesEarnRateByMemberId() {
        MemberGradeResponse response = new MemberGradeResponse();
        org.springframework.test.util.ReflectionTestUtils.setField(response, "earnRate", 0.025);
        given(memberClient.getMemberGrade(1L)).willReturn(response);

        assertThat(memberGradeCacheService.getEarnRate(1L)).isEqualTo(0.025);
        assertThat(memberGradeCacheService.getEarnRate(1L)).isEqualTo(0.025);

        verify(memberClient, times(1)).getMemberGrade(1L);
    }

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        OrderCreateMetrics orderCreateMetrics() {
            return new OrderCreateMetrics(new SimpleMeterRegistry());
        }

        @Bean
        String cacheTestMarker() {
            return "cache-test";
        }
    }
}
