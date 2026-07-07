package com.nhnacademy.member_server.entity.point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointEventType {

    EARN_ORDER("상품 구매 적립"),
    EARN_REVIEW("일반 리뷰 작성 적립"),
    EARN_PHOTO_REVIEW("포토 리뷰 작성 적립"),
    EARN_REVIEW_UPGRADE("리뷰 수정 추가 적립 (일반->포토)"),
    EARN_SIGNUP("회원가입 축하 적립"),
    EARN_ADMIN("관리자에 의한 적립"),

    // 적립 통계에 안들어감
    EARN_REFUND("상품 반품으로 인한 적립"),  // (반품시 돈 대신 포인트로)

    USE_ORDER("상품 결제 사용"),
    USE_ADMIN("관리자에 의한 차감"),

    EARN_CANCEL_RETURN("반품으로 인한 적립 포인트 회수"),

    USE_CANCEL_ORDER("주문 취소로 인한 사용 포인트 복구"),
    USE_CANCEL_RETURN("반품으로 인한 사용 포인트 복구");

    private final String description;
}