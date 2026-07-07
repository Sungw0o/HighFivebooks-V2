package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.request.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.request.CouponStatusRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import com.nhnacademy.coupon_server.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CouponAdminController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
@DisplayName("CouponAdminController 컨트롤러 테스트")
public class CouponAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 템플릿 생성 성공")
    void createCouponSuccess() throws Exception {
        CouponRequestDto couponRequestDto = CouponRequestDto.builder()
                .id(1L)
                .couponName("신규 쿠폰")
                .issueCount(100)
                .issueStartAt(LocalDateTime.now().plusDays(1))
                .issueEndAt(LocalDateTime.now().plusDays(2))
                .validPeriodDate(30)
                .build();

        CouponResponseDto responseDto = CouponResponseDto.builder()
                .id(100L)
                .couponPolicyId(1L)
                .couponName("신규 쿠폰")
                .issueCount(100)
                .build();

        when(couponService.create(any(CouponRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/coupons/admin/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(couponRequestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.couponName").value("신규 쿠폰"));
    }

    @Test
    @DisplayName("모든 쿠폰 템플릿 조회 성공")
    void findAllCouponSuccess() throws Exception {
        CouponResponseDto coupon1 = CouponResponseDto.builder()
                .id(101L)
                .couponName("여름 세일 쿠폰")
                .build();

        CouponResponseDto coupon2 = CouponResponseDto.builder()
                .id(102L)
                .couponName("겨울 세일 쿠폰")
                .build();

        when(couponService.findAll()).thenReturn(List.of(coupon1, coupon2));

        mockMvc.perform(get("/api/coupons/admin/coupons")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].couponName").value("여름 세일 쿠폰"))
                .andExpect(jsonPath("$[1].couponName").value("겨울 세일 쿠폰"));
    }

    @Test
    @DisplayName("쿠폰 상태 변경 성공")
    void updateCouponStatus_Success() throws Exception {
        Long couponId = 100L;
        CouponStatusRequestDto requestDto = new CouponStatusRequestDto(CouponStatus.ACTIVE);

        doNothing().when(couponService).updateCouponStatus(couponId, CouponStatus.ACTIVE);

        mockMvc.perform(post("/api/coupons/admin/coupons/{couponId}/change-status", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쿠폰 상태 변경 실패 - null 상태값")
    void updateCouponStatus_Fail_NullStatus() throws Exception {
        Long couponId = 100L;
        CouponStatusRequestDto requestDto = new CouponStatusRequestDto(null);

        mockMvc.perform(post("/api/coupons/admin/coupons/{couponId}/change-status", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
