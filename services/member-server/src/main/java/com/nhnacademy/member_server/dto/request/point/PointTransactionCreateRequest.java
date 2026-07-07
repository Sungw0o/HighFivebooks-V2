package com.nhnacademy.member_server.dto.request.point;

import com.nhnacademy.member_server.entity.point.PointEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointTransactionCreateRequest {
    private Long memberId;
    private Long amount;       // 적립/사용/환불할 금액 (양수)
    private Long orderId;      // 관련 주문 번호
    private PointEventType pointEventType; // EARN_ORDER, USE_ORDER, USE_CANCEL_ORDER 등

    // ★ 환불(USE_CANCEL_ORDER)일 때, 취소할 원본 PointHistory의 ID
    private Long originalPointHistoryId;
}