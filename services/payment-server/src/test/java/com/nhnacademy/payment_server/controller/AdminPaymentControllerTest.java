package com.nhnacademy.payment_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.dto.response.PaymentStatsResponse;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.service.PaymentStatService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPaymentController.class)
class AdminPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentStatService paymentStatService;

    @Test
    @DisplayName("종합 통계 조회 성공")
    void getSummary_Success() throws Exception {
        // given
        PaymentStatsResponse response = new PaymentStatsResponse(1000000L, 100000L, 900000L, 5L, 3L, 2L); // 적절한 생성자나 빌더 사용
        given(paymentStatService.getTotalStats()).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/payments/admin/stats/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("일별 통계 조회 성공 - 파라미터 없을 시 기본값(최근 7일) 적용")
    void getDailyStats_Success_DefaultParams() throws Exception {
        // given
        List<DailySalesResponse> response = Collections.emptyList();
        given(paymentStatService.getDailyStats(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/payments/admin/stats/daily")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("일별 통계 조회 성공 - 날짜 파라미터 지정")
    void getDailyStats_Success_WithParams() throws Exception {
        // given
        String startDate = LocalDate.now().minusDays(3).toString();
        String endDate = LocalDate.now().toString();

        List<DailySalesResponse> response = Collections.emptyList();
        given(paymentStatService.getDailyStats(any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/payments/admin/stats/daily")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("일별 통계 조회 실패 - 시작일이 종료일보다 늦음")
    void getDailyStats_Fail_InvalidDateRange() throws Exception {
        // given
        String startDate = "2024-01-10";
        String endDate = "2024-01-01"; // 종료일이 더 빠름

        // when & then
        mockMvc.perform(get("/api/payments/admin/stats/daily")
                        .param("startDate", startDate)
                        .param("endDate", endDate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(result -> {
                    // 컨트롤러에서 예외가 잘 던져지는지 확인
                    if (!(result.getResolvedException() instanceof BusinessException)) {
                        throw new AssertionError("BusinessException이 발생해야 합니다.");
                    }
                    BusinessException ex = (BusinessException) result.getResolvedException();
                    if (ex.getErrorCode() != ErrorCode.INVALID_DATE_RANGE) {
                        throw new AssertionError("ErrorCode가 일치하지 않습니다.");
                    }
                })
                .andDo(print());
    }
}