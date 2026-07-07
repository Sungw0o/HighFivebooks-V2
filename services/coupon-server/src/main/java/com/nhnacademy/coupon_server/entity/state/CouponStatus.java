package com.nhnacademy.coupon_server.entity.state;

public enum CouponStatus {
    ACTIVE,
    INACTIVE,
    EXPIRED,   // [추가] 만료 상태 추가
    SOLD_OUT,  // [선택] 소진 상태도 필요하다면 추가 (필요 없으면 생략)
    WAITING
}
