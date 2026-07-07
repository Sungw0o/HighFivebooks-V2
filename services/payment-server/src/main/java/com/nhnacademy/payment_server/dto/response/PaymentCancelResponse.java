package com.nhnacademy.payment_server.dto.response;

import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCancelResponse {

    @Schema(description = "취소된 결제 데이터의 DB ID", example = "10")
    private Long paymentId;

    @Schema(description = "결제 상태 (CANCELED)", example = "CANCELED")
    private PaymentStatus status;

    @Schema(description = "취소된 금액", example = "50000")
    private Long canceledAmount;

    @Schema(description = "취소 승인 일시", example = "2025-12-01T10:00:00")
    private LocalDateTime canceledAt;

    @Schema(description = "취소 사유", example = "단순 변심")
    private String cancelReason;

    public static PaymentCancelResponse from(Payment payment, String cancelReason) {
        return PaymentCancelResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .canceledAmount(payment.getAmount())
                .canceledAt(payment.getCancelledAt())
                .cancelReason(cancelReason)
                .build();
    }
}