package com.nhnacademy.coupon_server.dto.response;

import com.nhnacademy.coupon_server.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, String detailMessage) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(detailMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
