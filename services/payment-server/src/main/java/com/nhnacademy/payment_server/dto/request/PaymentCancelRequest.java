package com.nhnacademy.payment_server.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelRequest {

    @Schema(description = "취소할 결제 고유 키 (Toss)", example = "tviva2024...")
    private String paymentKey;

    @Schema(description = "취소 사유", example = "단순 변심")
    private String cancelReason;
}