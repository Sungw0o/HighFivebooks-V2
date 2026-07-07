package com.nhnacademy.coupon_server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    // Coupon Policy
    COUPON_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "CP001", "존재하지 않는 쿠폰 정책입니다."),

    // Coupon
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "CO001", "존재하지 않는 쿠폰입니다."),
    DUPLICATE_COUPON_ISSUE(HttpStatus.CONFLICT, "CO002", "이미 발급된 쿠폰입니다."),

    // Member Coupon
    MEMBER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "MC001", "회원 쿠폰을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
