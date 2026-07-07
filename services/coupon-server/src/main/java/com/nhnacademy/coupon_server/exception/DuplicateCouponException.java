package com.nhnacademy.coupon_server.exception;

public class DuplicateCouponException extends CouponServerException {

    public DuplicateCouponException() {
        super(ErrorCode.DUPLICATE_COUPON_ISSUE);
    }
}
