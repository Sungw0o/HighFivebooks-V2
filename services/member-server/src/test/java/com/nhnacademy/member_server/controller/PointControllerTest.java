package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointHistoryResponse;
import com.nhnacademy.member_server.service.PointService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointController.class)
@AutoConfigureMockMvc(addFilters = false)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PointService pointService;

    @Test
    @DisplayName("내 포인트 잔액 조회 성공")
    void getMyBalance_Success() throws Exception {
        Long memberId = 1L;
        PointBalanceResponse response = new PointBalanceResponse(memberId, 5000L, 15000L);

        given(pointService.getBalance(memberId)).willReturn(response);

        mockMvc.perform(get("/api/points/balance")
                        .header("X-USER-ID", memberId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(5000))
                .andExpect(jsonPath("$.totalEarnedPoint").value(15000));
    }

    @Test
    @DisplayName("내 포인트 이력 조회 성공 (페이징)")
    void getMyHistory_Success() throws Exception {
        Long memberId = 1L;

        PointHistoryResponse history1 = PointHistoryResponse.builder()
                .id(100L)
                .amount(500L)
                .description("상품 구매 적립")
                .currentPoint(5500L)
                .transactionDate(LocalDateTime.now())
                .orderId(123L)
                .build();

        PointHistoryResponse history2 = PointHistoryResponse.builder()
                .id(99L)
                .amount(-1000L)
                .description("상품 결제 사용")
                .currentPoint(5000L)
                .transactionDate(LocalDateTime.now().minusDays(1))
                .orderId(120L)
                .build();

        Page<PointHistoryResponse> historyPage = new PageImpl<>(List.of(history1, history2));

        given(pointService.getHistory(eq(memberId), any(Pageable.class))).willReturn(historyPage);

        mockMvc.perform(get("/api/points/history")
                        .header("X-USER-ID", memberId)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray()) // content가 배열인지
                .andExpect(jsonPath("$.content[0].amount").value(500))
                .andExpect(jsonPath("$.content[1].amount").value(-1000))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}
