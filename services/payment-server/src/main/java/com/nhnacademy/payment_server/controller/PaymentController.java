package com.nhnacademy.payment_server.controller;

import com.nhnacademy.payment_server.docs.PaymentSwagger;
import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController implements PaymentSwagger {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest requestDto){
        PaymentConfirmResponse responseDto = paymentService.confirmPayment(requestDto);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/{paymentKey}/cancel")
    public ResponseEntity<Void> cancelPayment(@PathVariable String paymentKey, @RequestBody PaymentCancelRequest request) {
        paymentService.cancelPayment(paymentKey, request);
        return ResponseEntity.ok().build();
    }
}