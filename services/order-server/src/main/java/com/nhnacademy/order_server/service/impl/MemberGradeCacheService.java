package com.nhnacademy.order_server.service.impl;

import com.nhnacademy.order_server.adapter.MemberClient;
import com.nhnacademy.order_server.dto.response.external.MemberGradeResponse;
import com.nhnacademy.order_server.observability.OrderCreateMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberGradeCacheService {

    private final MemberClient memberClient;
    private final OrderCreateMetrics orderCreateMetrics;

    @Cacheable(
            cacheNames = "memberEarnRate",
            key = "#userId",
            cacheManager = "memberGradeCacheManager",
            sync = true
    )
    public double getEarnRate(Long userId) {
        MemberGradeResponse response = orderCreateMetrics.record("member_grade_remote",
                () -> memberClient.getMemberGrade(userId));
        if (response == null || response.getEarnRate() == null) {
            throw new IllegalStateException("Member grade response does not contain earnRate");
        }
        return response.getEarnRate();
    }
}
