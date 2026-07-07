package com.nhnacademy.coupon_server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponCountVo implements CouponCountDto{
    private Long couponId;
    private Long count;
}
