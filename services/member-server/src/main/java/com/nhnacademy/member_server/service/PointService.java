package com.nhnacademy.member_server.service;

import com.nhnacademy.member_server.dto.request.point.PointAdminAdjustmentRequest;
import com.nhnacademy.member_server.dto.request.point.PointAdminPolicyRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.dto.response.point.PointAdminPolicyResponse;
import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointService {
    Long createTransaction(PointTransactionCreateRequest request);

    // --- 조회 메서드 ---
    PointBalanceResponse getBalance(Long memberId);
    Page<PointHistoryResponse> getHistory(Long memberId, Pageable pageable);

    // --- 관리자 정책 및 조정 ---
    PointAdminPolicyResponse getRecentPolicy();
    void updatePolicy(PointAdminPolicyRequest requestDto);
    Long adjustmentMemberPoint(PointAdminAdjustmentRequest requestDto);

    // --- TCC (분산 트랜잭션) 메서드 ---
    void reservePoint(Long memberId, Long amount, Long orderId);
    void confirmPoint(Long memberId, Long amount, Long orderId);
    void cancelPoint(Long memberId, Long amount, Long orderId);
}
