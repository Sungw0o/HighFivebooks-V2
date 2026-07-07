package com.nhnacademy.coupon_server.calculator;

import com.nhnacademy.coupon_server.entity.Coupon;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class CouponDateCalculatorTest {
    private final CouponDateCalculator couponDateCalculator = new CouponDateCalculator();

    @Test
    @DisplayName("만료일 계산 - 고정 만료일 우선 적용")
    void calculateExpirationFixedDate() {
        LocalDateTime fixedDate = LocalDateTime.of(2025,12,31,23,59,59);
        Coupon coupon = Coupon.builder()
                .validEndAt(fixedDate)
                .validPeriodDate(30)
                .build();

        LocalDateTime result = couponDateCalculator.calculateExpiration(coupon);
        Assertions.assertEquals(fixedDate, result);
    }

    @Test
    @DisplayName("만료일 계산 - 유효 기간 적용")
    void calculateExpirationPeriodDate() {
        int periodDays = 30;
        Coupon coupon = Coupon.builder()
                .validEndAt(null)
                .validPeriodDate(periodDays)
                .build();

        LocalDateTime result = couponDateCalculator.calculateExpiration(coupon);

        LocalDateTime expectedDate = LocalDateTime.now().plusDays(periodDays);
        LocalDateTime expected = expectedDate.with(LocalTime.MAX);

        Assertions.assertEquals(expected.toLocalDate(), result.toLocalDate());
        Assertions.assertEquals(expected.toLocalTime(), result.toLocalTime());
    }

    @Test
    @DisplayName("만료일 계산 실패 - 만료 정책 미설정")
    void calculateExpirationFail() {
        Coupon coupon = Coupon.builder()
                .validEndAt(null)
                .validPeriodDate(null)
                .build();

        Assertions.assertThrows(IllegalStateException.class, () -> couponDateCalculator.calculateExpiration(coupon));
    }
}
