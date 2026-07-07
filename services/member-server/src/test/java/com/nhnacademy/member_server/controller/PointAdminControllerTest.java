package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.point.PointAdminAdjustmentRequest;
import com.nhnacademy.member_server.dto.request.point.PointAdminPolicyRequest;
import com.nhnacademy.member_server.dto.response.point.PointAdminPolicyResponse;
import com.nhnacademy.member_server.service.PointService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class PointAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointService pointService;

    @Test
    @DisplayName("관리자: 포인트 정책 조회 성공")
    void getPolicy_Success() throws Exception {

        PointAdminPolicyResponse response = PointAdminPolicyResponse.builder()
                .signupPoint(5000)
                .reviewPoint(200)
                .photoPoint(500)
                .updatedAt(LocalDateTime.now())
                .build();

        given(pointService.getRecentPolicy()).willReturn(response);


        mockMvc.perform(get("/api/admin/points/policy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signupPoint").value(5000))
                .andExpect(jsonPath("$.reviewPoint").value(200));
    }

    @Test
    @DisplayName("관리자: 포인트 정책 수정 성공")
    void updatePolicy_Success() throws Exception {
        PointAdminPolicyRequest request = new PointAdminPolicyRequest(10000, 300, 600);

        mockMvc.perform(post("/api/admin/points/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(pointService).updatePolicy(any(PointAdminPolicyRequest.class));
    }

    @Test
    @DisplayName("관리자: 포인트 수동 조정 성공")
    void adjustmentMemberPoint_Success() throws Exception {
        Long memberId = 1L;
        Long adjustedAmount = 5000L;
        Long expectedBalance = 15000L;

        PointAdminAdjustmentRequest request = new PointAdminAdjustmentRequest(memberId, adjustedAmount, "보상 지급");

        given(pointService.adjustmentMemberPoint(any(PointAdminAdjustmentRequest.class)))
                .willReturn(expectedBalance);

        mockMvc.perform(post("/api/admin/points/adjustment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.currentPoint").value(expectedBalance));
    }
}