package com.nhnacademy.payment_server.controller;

import com.nhnacademy.payment_server.docs.PaymentMethodSwagger;
import com.nhnacademy.payment_server.dto.request.MethodStatusRequest;
import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import com.nhnacademy.payment_server.service.PaymentMethodService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/methods")
public class PaymentMethodController implements PaymentMethodSwagger {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> getAllMethods() {
        return ResponseEntity.ok(paymentMethodService.getAllMethods());
    }

    @PutMapping("/admin/{methodId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long methodId, @RequestBody MethodStatusRequest request) {
        paymentMethodService.updateStatus(methodId, request.isActive());
        return ResponseEntity.ok().build();
    }

}
