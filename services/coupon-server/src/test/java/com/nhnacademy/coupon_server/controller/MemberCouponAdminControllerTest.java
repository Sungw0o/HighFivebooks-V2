package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemberCouponAdminController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
@DisplayName("MemberCouponAdminController 컨트롤러 테스트")
public class MemberCouponAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberCouponService memberCouponService;

    @Test
    @DisplayName("회원 쿠폰 전체 조회 성공")
    void getMemberCouponsTestSuccess() throws Exception {
        MemberCouponResponseDto responseDto = MemberCouponResponseDto.builder()
                .id(1L)
                .userId(100L)
                .couponName("웰컴 쿠폰")
                .status(Status.ISSUED)
                .issuedAt(LocalDateTime.now())
                .build();

        Page<MemberCouponResponseDto> page = new PageImpl<>(List.of(responseDto));

        when(memberCouponService.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/coupons/admin/member-coupons")
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("웰컴 쿠폰"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("관리자 쿠폰 수동 발급 성공")
    void issueCouponByAdminTestSuccess() throws Exception {
        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder()
                .couponId(10L)
                .userId(100L)
                .build();

        doNothing().when(memberCouponService).issueCouponByAdmin(any(MemberCouponIssueRequestDto.class));

        mockMvc.perform(post("/api/coupons/admin/member-coupons/issue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
