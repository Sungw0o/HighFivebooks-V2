package com.nhnacademy.payment_server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD_REQUEST
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "P001", "잘못된 입력값입니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "P002", "결제 금액이 일치하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, "P003", "처리할 수 없는 결제 상태입니다."),
    ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "P004", "이미 취소된 결제입니다."),
    // 기능 없고 지원 예정도 아님
    UNSUPPORTED_METHOD(HttpStatus.BAD_REQUEST, "P005", "지원하지 않는 결제 수단입니다."),
    // 기능은 있는데 점검 중
    METHOD_DISABLED(HttpStatus.BAD_REQUEST, "P006", "현재 사용할 수 없는 결제 수단입니다."),
    // 기능 없고 지원 예정
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "P007", "추후 구현 예정인 결제 수단입니다."),

    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "P008", "존재하지 않는 결제 정보입니다."),
    METHOD_NOT_FOUND(HttpStatus.NOT_FOUND, "P009", "존재하지 않는 결제 수단입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "P010", "존재하지 않는 주문 정보입니다."),

    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "P011", "시작일은 종료일보다 이후일 수 없습니다."),

    // 500 INTERNAL_SERVER_ERROR
    TOSS_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "P500", "토스 페이먼츠 처리 중 오류가 발생했습니다."),
    POINT_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "P501", "포인트 처리 중 오류가 발생했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C001", "서버 내부 오류가 발생했습니다."),
    ORDER_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "주문 서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
