package com.nhnacademy.payment_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmRequest {
    @Schema(description = "Toss에서 받은 결제 식별 키", example = "tviva2024...")
    private String paymentKey;
    @Schema(description = "주문 서버에서 생성된 주문 번호", example = "w13sb-")
    private String orderKey;
    @Schema(description = "주문 서버에서 생성된 최종 승인 요청할 금액", example = "50000")
    private Long amount;
    @Schema(description = "결제 수단 (TOSS, POINT, ETC)", example = "TOSS")
    private String paymentMethod; // 내부 로직 용
}
