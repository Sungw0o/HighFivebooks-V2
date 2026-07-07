package com.nhnacademy.payment_server.dto.message;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessMessage {
    private Long orderId;        // 주문 번호
    private String paymentKey;   // (선택) 취소에 쓸수도
    private Long totalAmount;    // (선택) 검증에 필요하면 사용
    private LocalDateTime approvedAt; // 결제 일시
}