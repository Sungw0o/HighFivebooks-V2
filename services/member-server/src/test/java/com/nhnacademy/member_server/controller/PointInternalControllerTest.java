package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionRequest;
import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.entity.point.PointEventType;
import com.nhnacademy.member_server.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointInternalController.class)
@AutoConfigureMockMvc(addFilters = false) // Security 필터 해제
class PointInternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointService pointService;

    // 공통 URL 상수
    private static final String BASE_URL = "/internal/point-transactions";

    @Test
    @DisplayName("TCC: 포인트 적립 예약 (reserve) 성공")
    void reservePoint_Success() throws Exception {
        // given
        PointTransactionRequest request = new PointTransactionRequest(1L, 1000L, 100L);

        // when & then
        mockMvc.perform(post(BASE_URL + "/tcc/reserve")
                        .contentType(MediaType.APPLICATION_JSON) // ★ @RequestBody니까 JSON 필수
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // verify
        then(pointService).should().reservePoint(request.getMemberId(), request.getAmount(), request.getOrderId());
    }

    @Test
    @DisplayName("TCC: 포인트 적립 확정 (confirm) 성공")
    void confirmPoint_Success() throws Exception {
        // given
        PointTransactionRequest request = new PointTransactionRequest(1L, 1000L, 100L);

        // when & then
        mockMvc.perform(post(BASE_URL + "/tcc/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        then(pointService).should().confirmPoint(request.getMemberId(), request.getAmount(), request.getOrderId());
    }

    @Test
    @DisplayName("TCC: 포인트 사용 취소 (cancel) 성공")
    void cancelPoint_Success() throws Exception {
        // given
        PointTransactionRequest request = new PointTransactionRequest(1L, 500L, 100L);

        // when & then
        mockMvc.perform(post(BASE_URL + "/tcc/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        then(pointService).should().cancelPoint(request.getMemberId(), request.getAmount(), request.getOrderId());
    }

    @Test
    @DisplayName("포인트 잔액 조회 성공")
    void getPointBalance_Success() throws Exception {
        Long memberId = 1L;
        PointBalanceResponse balanceResponse = new PointBalanceResponse(memberId, 2500L, 10000L);

        given(pointService.getBalance(memberId)).willReturn(balanceResponse);

        mockMvc.perform(get(BASE_URL + "/{memberId}", memberId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(2500))
                .andExpect(jsonPath("$.totalEarnedPoint").value(10000));

        then(pointService).should().getBalance(memberId);
    }

    @Test
    @DisplayName("통합 API: 반품 적립 회수 (EARN_CANCEL_RETURN)")
    void createTransaction_EarnCancel_Success() throws Exception {
        // given
        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(1L)
                .amount(500L)
                .orderId(200L)
                .pointEventType(PointEventType.EARN_CANCEL_RETURN)
                .build();

        given(pointService.createTransaction(any(PointTransactionCreateRequest.class)))
                .willReturn(1000L); // 잔액 리턴 가정

        // when
        mockMvc.perform(post(BASE_URL) // 통합 엔드포인트 호출
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(1000));

        // then
        then(pointService).should().createTransaction(any(PointTransactionCreateRequest.class));
    }

    @Test
    @DisplayName("통합 API: 반품 사용 복구 (USE_CANCEL_RETURN)")
    void createTransaction_UseCancel_Success() throws Exception {
        // given
        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(1L)
                .amount(1000L)
                .orderId(200L)
                .pointEventType(PointEventType.USE_CANCEL_RETURN)
                .build();

        given(pointService.createTransaction(any(PointTransactionCreateRequest.class)))
                .willReturn(5000L);

        // when
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(5000));

        // then
        then(pointService).should().createTransaction(any(PointTransactionCreateRequest.class));
    }
}