package com.nhnacademy.payment_server.adaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment_server.config.TossPaymentConfig;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@RestClientTest(TossPaymentAdapter.class)
class TossPaymentAdapterTest {

    @Autowired
    private TossPaymentAdapter tossPaymentAdapter;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TossPaymentConfig tossPaymentConfig;

    private String encodedAuthHeader;

    @BeforeEach
    void setUp() {
        String testSecretKey = "test_sk";
        given(tossPaymentConfig.getSecretKey()).willReturn(testSecretKey);

        String keyWithColon = testSecretKey + ":";
        encodedAuthHeader = "Basic " + Base64.getEncoder().encodeToString(keyWithColon.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class Config {

        @Bean
        public RestClient restClient(RestClient.Builder builder) {
            return builder.build();
        }
    }

    @Test
    @DisplayName("결제 승인 요청 성공")
    void requestConfirm_Success() throws JsonProcessingException {
        String paymentKey = "test_pay_key";
        String orderId = "order_123";
        Long amount = 50000L;

        TossConfirmResponse mockResponse = TossConfirmResponse.builder()
                .paymentKey(paymentKey)
                .status("DONE")
                .totalAmount(amount)
                .method("카드")
                .build();

        mockServer.expect(requestTo("/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, encodedAuthHeader))
                .andExpect(jsonPath("$.paymentKey").value(paymentKey))
                .andExpect(jsonPath("$.amount").value(amount))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(mockResponse),
                        MediaType.APPLICATION_JSON
                ));

        TossConfirmResponse result = tossPaymentAdapter.requestConfirm(paymentKey, orderId, amount);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("DONE");
        assertThat(result.getPaymentKey()).isEqualTo(paymentKey);

        mockServer.verify();
    }

    @Test
    @DisplayName("결제 취소 요청 성공")
    void requestCancel_Success() {
        String paymentKey = "test_pay_key";
        String reason = "단순 변심";

        mockServer.expect(requestTo("/v1/payments/" + paymentKey + "/cancel"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, encodedAuthHeader))
                .andExpect(jsonPath("$.cancelReason").value(reason))
                .andRespond(withSuccess());

        tossPaymentAdapter.requestCancel(paymentKey, reason);

        mockServer.verify();
    }
}