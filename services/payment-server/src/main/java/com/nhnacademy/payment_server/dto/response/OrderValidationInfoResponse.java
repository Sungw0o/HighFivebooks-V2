package com.nhnacademy.payment_server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderValidationInfoResponse {
    private Long orderId;
    private Long paymentAmount;
}