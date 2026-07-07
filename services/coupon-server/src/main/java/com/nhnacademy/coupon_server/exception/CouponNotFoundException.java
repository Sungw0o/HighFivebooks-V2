package com.nhnacademy.coupon_server.exception;

public class CouponNotFoundException extends CouponServerException {

    public CouponNotFoundException() {
        super(ErrorCode.COUPON_NOT_FOUND);
    }

}
