package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.service.CouponService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CouponController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("특정 도서에 적용 가능한 쿠폰 목록 조회")
    void getBookCoupons_Success() throws Exception {
        Long bookId = 123L;
        CouponResponseDto couponDto = CouponResponseDto.builder()
                .id(1L)
                .couponName("도서 전용 쿠폰")
                .status("ACTIVE")
                .build();

        when(couponService.getCouponsForProduct(eq(bookId), any()))
                .thenReturn(List.of(couponDto));

        mockMvc.perform(get("/api/coupons/books/{book-id}", bookId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].couponName").value("도서 전용 쿠폰"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("도서 상세 조회용 쿠폰 목록 조회 (범용 쿠폰 제외)")
    void getBookCoupons_Detail_ExcludeGlobal() throws Exception {
        Long bookId = 123L;
        CouponResponseDto couponDto = CouponResponseDto.builder()
                .id(2L)
                .couponName("범용 제외 쿠폰")
                .status("ACTIVE")
                .build();

        // include-global=false 일 때는 getBookSpecificCoupons가 호출됩니다.
        when(couponService.getBookSpecificCoupons(eq(bookId), any()))
                .thenReturn(List.of(couponDto));

        mockMvc.perform(get("/api/coupons/books/{book-id}", bookId)
                        .param("include-global", "false") // 파라미터 설정
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].couponName").value("범용 제외 쿠폰"));
    }
}