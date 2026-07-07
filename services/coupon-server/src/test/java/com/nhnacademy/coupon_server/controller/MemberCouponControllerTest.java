package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.request.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.CouponServerException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
import com.nhnacademy.coupon_server.service.CouponService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = MemberCouponController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
class MemberCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberCouponService memberCouponService;

    @MockitoBean
    private CouponService couponService;

    @Test
    @DisplayName("사용자 쿠폰 발급 성공 (201)")
    void issueCouponSuccess() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doNothing().when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/api/coupons/issue")
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 이미 발급된 쿠폰 (409)")
    void issueCouponFailureDuplicateCoupon() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doThrow(new DuplicateCouponException())
                .when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/api/coupons/issue")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_COUPON_ISSUE.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_COUPON_ISSUE.getMessage()));
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 발급 기간 아님/수량 소진 (409)")
    void issueCouponFailureBadRequest() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(couponId);

        doThrow(new CouponServerException(ErrorCode.INVALID_INPUT_VALUE))
                .when(memberCouponService).issueCouponByUser(userId, couponId);

        mockMvc.perform(post("/api/coupons/issue")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()));
    }

    @Test
    @DisplayName("사용자 쿠폰 발급 실패 - 유효하지 않은 요청(ID 누락)")
    void issueCouponFailureInvalidRequest() throws Exception {
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(null);

        mockMvc.perform(post("/api/coupons/issue")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자 쿠폰 조회 성공")
    void getCouponByUserIdSuccess() throws Exception {
        Long userId = 1L;

        MemberCouponResponseDto responseDto = MemberCouponResponseDto.builder()
                .couponName("테스트 쿠폰")
                .build();

        Page<MemberCouponResponseDto> pageResponse = new PageImpl<>(List.of(responseDto));

        when(memberCouponService.findCouponByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/coupons/members")
                .header("X-USER-ID", userId)
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("테스트 쿠폰"));
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 템플릿 목록 조회 성공")
    void getIssuableCouponsSuccess() throws Exception {
        CouponResponseDto limitedCoupon = CouponResponseDto.builder()
                .id(1L)
                .couponName("선착순 쿠폰")
                .remainingCount(90) // 잔여 수량 설정
                .build();

        CouponResponseDto unlimitedCoupon = CouponResponseDto.builder()
                .id(2L)
                .couponName("무제한 쿠폰")
                .remainingCount(null)
                .build();

        Page<CouponResponseDto> mockPage = new PageImpl<>(List.of(limitedCoupon, unlimitedCoupon));

        when(couponService.findIssuableCoupons(any(Pageable.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/api/coupons/templates")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].couponName").value("선착순 쿠폰"))
                .andExpect(jsonPath("$.content[0].remainingCount").value(90))
                .andExpect(jsonPath("$.content[1].couponName").value("무제한 쿠폰"))
                .andExpect(jsonPath("$.content[1].remainingCount").doesNotExist())
                .andDo(print());
    }

    @Test
    @DisplayName("주문 시 적용 가능 쿠폰 조회 성공")
    void getUsableCouponsSuccess() throws Exception {
        Long userId = 1L;

        MemberCouponResponseDto responseDto = MemberCouponResponseDto.builder()
                .couponName("주문 할인 쿠폰")
                .status(Status.ISSUED)
                .build();

        when(memberCouponService.findUsableCoupons(eq(userId), any(), any())).thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/coupons/members/order")
                .header("X-USER-ID", userId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].couponName").value("주문 할인 쿠폰"))
                .andExpect(jsonPath("$[0].status").value(Status.ISSUED.name()))
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 사용 처리 성공 (200)")
    void useCouponSuccess() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderId = 20251127L;

        MemberCouponUseRequestDto requestDto = new MemberCouponUseRequestDto(couponId, orderId);

        doNothing().when(memberCouponService).useCoupon(any(Long.class), any(MemberCouponUseRequestDto.class));

        mockMvc.perform(post("/api/coupons/use", userId)
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 사용 처리 실패 - 필수 값 누락 (400)")
    void useCouponInvalidInput() throws Exception {
        Long userId = 1L;

        MemberCouponUseRequestDto requestDto = new MemberCouponUseRequestDto(100L, null);

        mockMvc.perform(post("/api/coupons/use", userId)
                .header("X-USER-ID", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("쿠폰 사용 취소 성공 (200 OK)")
    void cancelCouponUsageSuccess() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        Long orderId = 12345L;
        MemberCouponCancelRequestDto requestDto = new MemberCouponCancelRequestDto(couponId, orderId);

        doNothing().when(memberCouponService).cancelCouponUsage(any(Long.class), any(MemberCouponCancelRequestDto.class));

        mockMvc.perform(post("/api/coupons/cancel")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쿠폰 할인 금액 계산 성공 (200)")
    void calculateCouponSuccess() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        Long totalOrderPrice = 30000L;
        Long expectedDiscount = 5000L;
        Long expectedFinalPrice = 25000L;

        CouponCalculationRequestDto requestDto = new CouponCalculationRequestDto(couponId, totalOrderPrice);

        CouponCalculationResponseDto responseDto = CouponCalculationResponseDto.builder()
                .discountAmount(expectedDiscount)
                .finalPrice(expectedFinalPrice)
                .build();

        when(memberCouponService.calculateDiscount(eq(userId), any(CouponCalculationRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/coupons/calculate")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountAmount").value(expectedDiscount))
                .andExpect(jsonPath("$.finalPrice").value(expectedFinalPrice));
    }

    @Test
    @DisplayName("쿠폰 할인 계산 실패 - 유효하지 않은 주문 금액 (400)")
    void calculateCouponFailureInvalidOrderPrice() throws Exception {
        Long userId = 1L;
        Long couponId = 100L;
        Long invalidPrice = -1000L; // 또는 0L

        CouponCalculationRequestDto requestDto = new CouponCalculationRequestDto(couponId, invalidPrice);

        mockMvc.perform(post("/api/coupons/calculate")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 예외 처리 시나리오 (404 Not Found)")
    void issueCouponFailure_CouponNotFound_Scenario() throws Exception {
        Long userId = 1L;
        Long invalidCouponId = 999L;
        UserCouponIssueRequestDto requestDto = new UserCouponIssueRequestDto(invalidCouponId);

        doThrow(new CouponNotFoundException())
                .when(memberCouponService).issueCouponByUser(userId, invalidCouponId);

        mockMvc.perform(post("/api/coupons/issue")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CO001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 쿠폰입니다."));
    }

    @Test
    @DisplayName("쿠폰 할인 계산 실패 - 쿠폰 ID null (400)")
    void calculateCouponFailureNullCouponId() throws Exception {
        CouponCalculationRequestDto requestDto = new CouponCalculationRequestDto(null, 10000L);

        mockMvc.perform(post("/api/coupons/calculate")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("쿠폰 할인 계산 실패 - 0원 주문 (400)")
    void calculateCouponFailureZeroOrderPrice() throws Exception {
        CouponCalculationRequestDto requestDto = new CouponCalculationRequestDto(100L, 0L);

        mockMvc.perform(post("/api/coupons/calculate")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("쿠폰 할인 계산 실패 - 존재하지 않는 쿠폰 (404)")
    void calculateCouponFailureNonExistentCoupon() throws Exception {
        Long userId = 1L;
        Long invalidCouponId = 999L;
        CouponCalculationRequestDto requestDto = new CouponCalculationRequestDto(invalidCouponId, 10000L);

        doThrow(new CouponNotFoundException())
                .when(memberCouponService).calculateDiscount(eq(userId), any(CouponCalculationRequestDto.class));

        mockMvc.perform(post("/api/coupons/calculate")
                        .header("X-USER-ID", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNotFound());
    }
}
