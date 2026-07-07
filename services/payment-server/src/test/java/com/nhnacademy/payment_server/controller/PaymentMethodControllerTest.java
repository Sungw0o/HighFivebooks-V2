package com.nhnacademy.payment_server.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment_server.dto.request.MethodStatusRequest;
import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import com.nhnacademy.payment_server.service.PaymentMethodService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentMethodController.class)
@AutoConfigureMockMvc(addFilters = false) // 시큐리티 필터 무시
class PaymentMethodControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean // 서비스는 가짜 객체로 대체
    private PaymentMethodService paymentMethodService;

    @Test
    @DisplayName("결제 수단 조회 성공")
    void getActiveMethods_Success() throws Exception {
        // given
        PaymentMethodResponse toss = new PaymentMethodResponse(1L, "TOSS", "토스", true);
        PaymentMethodResponse bankTransfer = new PaymentMethodResponse(2L, "BANK_TRANSFER", "무통장입금", true);

        given(paymentMethodService.getAllMethods()).willReturn(List.of(toss, bankTransfer));

        // when & then
        mockMvc.perform(get("/api/payments/methods")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("TOSS"));
    }

    @Test
    @DisplayName("관리자: 결제 수단 상태 변경 성공")
    void updateStatus_Success() throws Exception {
        // given
        Long methodId = 1L;
        MethodStatusRequest request = new MethodStatusRequest();
        request.setActive(false); // (Setter 필요 or Builder)

        // when & then
        mockMvc.perform(put("/api/payments/methods/admin/{methodId}/status", methodId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // 서비스가 올바른 인자로 호출되었는지 검증
        verify(paymentMethodService).updateStatus(methodId, false);
    }
}