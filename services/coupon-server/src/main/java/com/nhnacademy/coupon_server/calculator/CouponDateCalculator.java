package com.nhnacademy.coupon_server.calculator;

import com.nhnacademy.coupon_server.entity.Coupon;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class CouponDateCalculator {
    // 쿠폰 유효기간 관련 로직 추가
    public LocalDateTime calculateExpiration(Coupon coupon) {
        if (coupon.getValidEndAt() != null) {
            return coupon.getValidEndAt();
        }

        if (coupon.getValidPeriodDate() != null && coupon.getValidPeriodDate() >= 0) {
            return LocalDateTime.now()
                    .plusDays(coupon.getValidPeriodDate())
                    .with(LocalTime.MAX);
        }

        throw new IllegalStateException("쿠폰의 유효기간 정책이 설정되지 않았습니다. (couponId=" + coupon.getId() + ")");
    }
}
