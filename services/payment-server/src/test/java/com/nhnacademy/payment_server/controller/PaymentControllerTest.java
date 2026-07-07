package com.nhnacademy.payment_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean PaymentService paymentService;

    @Test
    @DisplayName("결제 승인 요청 성공")
    void confirmPayment_Success() throws Exception {
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest(
                "key", "order_uuid", 50000L, "TOSS"
        );

        PaymentConfirmResponse response = PaymentConfirmResponse.builder()
                .paymentId(1L)
                .status(com.nhnacademy.payment_server.entity.PaymentStatus.DONE)
                .amount(50000L)
                .build();

        given(paymentService.confirmPayment(any(PaymentConfirmRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.amount").value(50000));
    }

    @Test
    @DisplayName("결제 취소 요청 성공")
    void cancelPayment_Success() throws Exception {
        PaymentCancelRequest request = new PaymentCancelRequest("key", "변심");

        // void 메서드 mock
        doAnswer(invocation -> null)
                .when(paymentService)
                .cancelPayment(eq("key"), any(PaymentCancelRequest.class));

        mockMvc.perform(post("/api/payments/{paymentKey}/cancel", "key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(paymentService).cancelPayment(eq("key"), any(PaymentCancelRequest.class));
    }


}