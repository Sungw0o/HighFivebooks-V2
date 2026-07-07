package com.nhnacademy.payment_server.dto.response;


import com.nhnacademy.payment_server.entity.PaymentStatus;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TossConfirmResponse {
    // Toss가 주는 JSON 응답을 받기위한 DTO
    private String status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime approvedAt;
    private String orderId;
    private String paymentKey;
    private Long totalAmount;
    private String method;

    public PaymentStatus getStatusEnum() {
        if (this.status == null) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(this.status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}