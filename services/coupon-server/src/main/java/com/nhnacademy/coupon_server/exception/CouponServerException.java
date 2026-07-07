package com.nhnacademy.coupon_server.exception;

import lombok.Getter;

@Getter
public class CouponServerException extends RuntimeException {
    private final ErrorCode errorCode;
    public CouponServerException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
