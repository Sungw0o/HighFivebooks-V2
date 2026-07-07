package com.nhnacademy.coupon_server.exception;

public class CouponPolicyNotFoundException extends CouponServerException {

    public CouponPolicyNotFoundException() {
        super(ErrorCode.COUPON_POLICY_NOT_FOUND);
    }
}
