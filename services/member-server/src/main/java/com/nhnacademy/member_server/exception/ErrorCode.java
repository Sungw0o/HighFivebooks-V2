package com.nhnacademy.member_server.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // C는 공통 오류, CT는 카트 오류, A는 권한 오류, B는 책 오류, EXT는 책 서버 오류

    // Common
    DATABASE_ERROR(HttpStatus.FORBIDDEN, "C000" , "DB 오류입니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST,"C003" ,"요청 방식이 잘못됐습니다." ),
    // Cart
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, "CT001", "해당 장바구니를 찾을 수 없습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CT002", "장바구니에 해당 상품이 존재하지 않습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "CT003", "수량은 1개 이상이어야 합니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "IP001" , "장바구니에는 최대 100개까지만 담을 수 있습니다."),

    // Auth
    CART_ACCESS_DENIED(HttpStatus.FORBIDDEN, "A001", "해당 장바구니에 대한 접근 권한이 없습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A002", "아이디 또는 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않거나 만료된 토큰입니다."),

    // External (Book Service)
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "B001", "존재하지 않는 책입니다."),
    BOOK_SERVICE_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "B002", "도서 서비스 응답이 지연되고 있습니다."),

    // External (외부 서비스 관련)
    EXTERNAL_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "EXT001", "외부 서비스 통신 중 오류가 발생했습니다."),
    BOOK_NOT_FOUND_IN_SERVER(HttpStatus.NOT_FOUND, "EXT002", "도서 서비스에서 해당 책을 찾을 수 없습니다."),

    // Redis
    REDIS_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "RD001", "Redis 서버 통신 중 오류가 발생했습니다."),

    // Book
    BOOK_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"B001" ,"Book 서버에 문제가 생겼습니다." ),

    // Point (포인트 관련)
    POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "포인트 정보를 찾을 수 없습니다."),
    POINT_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "P002", "포인트 잔액이 부족합니다."),
    POINT_NOT_ORDER_ID(HttpStatus.BAD_REQUEST, "P003", "주문 포인트 적립 시 주문번호가 존재해야 합니다"),
    POINT_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "포인트 정책이 설정되지 않았습니다"),

    // [Address] 배송지 (새로 추가)
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "AD001", "존재하지 않는 배송지입니다."),
    ADDRESS_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AD002", "해당 배송지에 대한 권한이 없습니다."),
    MAX_ADDRESS_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "AD003", "배송지는 최대 10개까지만 등록할 수 있습니다."),
    DEFAULT_ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "AD004", "설정된 기본 배송지가 없습니다."),

    // [Member]
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "존재하지 않는 회원입니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "M010", "이미 존재하는 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "M011", "이미 존재하는 이메일입니다."),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "M012", "이미 존재하는 번호입니다."),
    BIRTHDATE_CANNOT_CHANGE(HttpStatus.BAD_REQUEST, "M020", "생년월일은 가입 완료 후 변경할 수 없습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "M004", "인증번호가 만료되거나 일치하지 않습니다."),
    MEMBER_DORMANT(HttpStatus.FORBIDDEN, "M005", "휴면 계정입니다. 본인 인증 후 해제해주세요."),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "M006", "탈퇴한 계정입니다."),
    MEMBER_NOT_DORMANT(HttpStatus.BAD_REQUEST, "M013", "해당 계정은 휴면 상태가 아닙니다."),

    // [Email]
    MAIL_SEND_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "메일 발송 중 오류가 발생했습니다."),


    // [PAYCO]
    PAYCO_TOKEN_ISSUE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "EXT010", "PAYCO 토큰 발급에 실패했습니다."),
    PAYCO_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "EXT011", "PAYCO API 호출 중 오류가 발생했습니다."),
    PAYCO_MEMBER_INFO_EMPTY(HttpStatus.BAD_GATEWAY, "EXT012", "PAYCO 회원 정보가 정상적으로 전달되지 않았습니다."),

    // [Encryption]
    ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E002", "암호화 처리 중 오류가 발생했습니다."),
    DECRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E003", "복호화 처리 중 오류가 발생했습니다."),

    // [SHA-256]
    SHA256_ALGORITHM_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "E004", "SHA-256 알고리즘을 찾을 수 없습니다."),
    SHA256_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E005", "SHA-256 암호화 중 오류가 발생했습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
