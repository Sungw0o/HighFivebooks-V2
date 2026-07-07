package com.nhnacademy.coupon_server.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CouponCalculationResponseDto {
    private Long discountAmount;
    private Long finalPrice;
}
