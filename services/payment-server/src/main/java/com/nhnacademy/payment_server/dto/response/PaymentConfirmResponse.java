package com.nhnacademy.payment_server.dto.response;

import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmResponse {

    @Schema(description = "최종 승인 완료된 결제 번호", example = "1")
    private Long paymentId;
    @Schema(description = "결제 상태", example = "DONE")
    private PaymentStatus status;
    @Schema(description = "결제 완료된 금액(영수증)", example = "50000")
    private Long amount;
    @Schema(description = "주문 번호", example = "1")
    private Long orderId;

    public static PaymentConfirmResponse from(Payment payment) {
        return PaymentConfirmResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .orderId(payment.getOrderId())
                .build();
    }
}
