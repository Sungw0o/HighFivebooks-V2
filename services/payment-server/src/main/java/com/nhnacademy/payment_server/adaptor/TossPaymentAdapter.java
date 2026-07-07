package com.nhnacademy.payment_server.adaptor;

import com.nhnacademy.payment_server.config.TossPaymentConfig;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TossPaymentAdapter {

    private final RestClient tossRestClient;
    private final TossPaymentConfig tossPaymentConfig; // 시크릿키

    // 결제 승인 요청
    public TossConfirmResponse requestConfirm(String paymentKey, String orderId, Long amount) {
        return tossRestClient.post()
                .uri("/v1/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .retrieve()
                .body(TossConfirmResponse.class);
    }

    // 결제 취소 요청
    public void requestCancel(String paymentKey, String reason) {
        tossRestClient.post()
                .uri("/v1/payments/" + paymentKey + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("cancelReason", reason))
                .retrieve()
                .toBodilessEntity();
    }

    // 헤더 생성 (private)
    private String getAuthHeader() {
        String secretKey = tossPaymentConfig.getSecretKey() + ":";
        return "Basic " + Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}