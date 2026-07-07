package com.nhnacademy.coupon_server.dto;

import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberCouponResponseDtoTest {

    @Test
    @DisplayName("DTO 변환 성공 - 최소 주문 금액 있음(포맷팅 확인) & 만료일 미래")
    void fromEntity_WithMinOrderValueAndFutureExpiration() {
        long minOrderValue = 15000L;
        LocalDateTime expiredAt = LocalDateTime.now().plusDays(5);

        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(minOrderValue) // 15,000원
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();

        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName("Test Coupon")
                .build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .id(1L)
                .userId(100L)
                .coupon(coupon)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .build();

        MemberCouponResponseDto dto = MemberCouponResponseDto.fromEntity(memberCoupon);
        // 1. 최소 주문 금액 포맷팅 검증 (#,### 패턴)
        assertEquals("15,000원 이상 구매 시 사용 가능", dto.getCondition());
        
        // 2. 남은 날짜 계산 검증 (5일)
        // 테스트 실행 시점에 따라 정확히 5일 차이가 나야 함
        assertEquals(5L, dto.getDaysRemaining());
        assertEquals(memberCoupon.getId(), dto.getId());
        assertEquals("Test Coupon", dto.getCouponName());
    }

    @Test
    @DisplayName("DTO 변환 성공 - 최소 주문 금액 0원 (조건 없음)")
    void fromEntity_WithZeroMinOrderValue() {
        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(0L)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10L)
                .build();

        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName("Zero Condition Coupon")
                .build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        MemberCouponResponseDto dto = MemberCouponResponseDto.fromEntity(memberCoupon);

        assertEquals("조건 없음", dto.getCondition());
    }

    @Test
    @DisplayName("DTO 변환 성공 - 최소 주문 금액 Null (조건 없음)")
    void fromEntity_WithNullMinOrderValue() {
        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(null) // null
                .discountType(DiscountType.FIXED)
                .discountValue(5000L)
                .build();

        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName("Null Condition Coupon")
                .build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        MemberCouponResponseDto dto = MemberCouponResponseDto.fromEntity(memberCoupon);

        assertEquals("조건 없음", dto.getCondition());
    }

    @Test
    @DisplayName("DTO 변환 성공 - 만료일이 오늘(D-Day)")
    void fromEntity_ExpiresToday() {
        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(10000L)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();

        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName("Today Expire Coupon")
                .build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                // 만료일이 현재 시간 (날짜 차이 0일)
                .expiredAt(LocalDateTime.now()) 
                .build();

        MemberCouponResponseDto dto = MemberCouponResponseDto.fromEntity(memberCoupon);

        assertEquals(0L, dto.getDaysRemaining());
    }

    @Test
    @DisplayName("DTO 변환 성공 - 만료일이 Null인 경우")
    void fromEntity_WithNullExpiration() {
        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(10000L)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();

        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName("No Expiration Coupon")
                .build();

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .expiredAt(null) // 만료일 없음
                .build();

        MemberCouponResponseDto dto = MemberCouponResponseDto.fromEntity(memberCoupon);

        assertEquals(0L, dto.getDaysRemaining());
    }
}