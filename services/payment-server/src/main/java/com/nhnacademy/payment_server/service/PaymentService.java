package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.PaymentCancelResponse;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;

public interface PaymentService {
    /**
     * 금액 검증 (Order Server)
     * 포인트 사용 (Member Server)
     * Toss API 승인 요청 (Toss Server)
     * DB 저장 (Payment Server)
     * 롤백 처리 (Toss 실패 시 포인트 환불)
     */
    PaymentConfirmResponse confirmPayment(PaymentConfirmRequest requestDto);
    PaymentCancelResponse cancelPayment(String paymentKey, PaymentCancelRequest requestDto);
}
