package com.nhnacademy.coupon_server.dto.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage {
    private Long memberId;
    private Long couponId;
}
