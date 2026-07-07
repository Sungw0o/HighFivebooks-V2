package com.nhnacademy.coupon_server.listener;

import com.nhnacademy.coupon_server.dto.message.CouponIssueMessage;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.dao.TransientDataAccessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponMessageListenerTest {

    @Mock
    private MemberCouponService memberCouponService;

    @InjectMocks
    private CouponMessageListener couponMessageListener;

    // ==========================================
    // 1. 웰컴 쿠폰 (receiveWelcomeCouponRequest)
    // ==========================================
    @Test
    @DisplayName("웰컴 쿠폰: 정상 수신 및 발급 성공")
    void receiveWelcomeCouponRequest_Success() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId, null);

        doNothing().when(memberCouponService).issueWelcomeCoupon(memberId);

        couponMessageListener.receiveWelcomeCouponRequest(message);

        verify(memberCouponService, times(1)).issueWelcomeCoupon(memberId);
    }

    @Test
    @DisplayName("웰컴 쿠폰: 일반 예외 발생 시 로그 남기고 종료 (ACK)")
    void receiveWelcomeCouponRequest_GeneralException() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId, null);

        doThrow(new RuntimeException("DB 연결 안됨 등")).when(memberCouponService).issueWelcomeCoupon(memberId);

        assertDoesNotThrow(() -> couponMessageListener.receiveWelcomeCouponRequest(message));

        verify(memberCouponService, times(1)).issueWelcomeCoupon(memberId);
    }

    @Test
    @DisplayName("웰컴 쿠폰: 일시적 오류(TransientDataAccessException) 시 예외 던짐 (NACK/Retry)")
    void receiveWelcomeCouponRequest_TransientException() {
        Long memberId = 1L;
        CouponIssueMessage message = new CouponIssueMessage(memberId, null);

        doThrow(mock(TransientDataAccessException.class)).when(memberCouponService).issueWelcomeCoupon(memberId);

        assertThrows(TransientDataAccessException.class, () ->
                couponMessageListener.receiveWelcomeCouponRequest(message)
        );
    }

    // ==========================================
    // 2. 일반 쿠폰 발급 (receiveIssueCouponRequest)
    // ==========================================
    @Test
    @DisplayName("일반 쿠폰: 정상 수신 및 생성 성공")
    void receiveIssueCouponRequest_Success() {
        Long userId = 1L;
        Long couponId = 100L;
        CouponIssueMessage message = new CouponIssueMessage(userId, couponId);

        doNothing().when(memberCouponService).createMemberCoupon(userId, couponId);

        couponMessageListener.receiveIssueCouponRequest(message);

        verify(memberCouponService, times(1)).createMemberCoupon(userId, couponId);
    }

    @Test
    @DisplayName("일반 쿠폰: 중복 발급 예외 발생 시 DLQ 이동 (AmqpRejectAndDontRequeueException)")
    void receiveIssueCouponRequest_Duplicate_DLQ() {
        Long userId = 1L;
        Long couponId = 100L;
        CouponIssueMessage message = new CouponIssueMessage(userId, couponId);

        doThrow(new DuplicateCouponException()).when(memberCouponService).createMemberCoupon(userId, couponId);

        assertThrows(AmqpRejectAndDontRequeueException.class, () ->
                couponMessageListener.receiveIssueCouponRequest(message)
        );
    }

    @Test
    @DisplayName("일반 쿠폰: 쿠폰 없음 예외 발생 시 DLQ 이동 (AmqpRejectAndDontRequeueException)")
    void receiveIssueCouponRequest_NotFound_DLQ() {
        Long userId = 1L;
        Long couponId = 999L;
        CouponIssueMessage message = new CouponIssueMessage(userId, couponId);

        doThrow(new CouponNotFoundException()).when(memberCouponService).createMemberCoupon(userId, couponId);

        assertThrows(AmqpRejectAndDontRequeueException.class, () ->
                couponMessageListener.receiveIssueCouponRequest(message)
        );
    }

    @Test
    @DisplayName("일반 쿠폰: 시스템 예외 발생 시 재시도 (Exception 던짐)")
    void receiveIssueCouponRequest_SystemException_Retry() {
        Long userId = 1L;
        Long couponId = 100L;
        CouponIssueMessage message = new CouponIssueMessage(userId, couponId);

        doThrow(new RuntimeException("시스템 오류")).when(memberCouponService).createMemberCoupon(userId, couponId);

        assertThrows(RuntimeException.class, () ->
                couponMessageListener.receiveIssueCouponRequest(message)
        );
    }
}