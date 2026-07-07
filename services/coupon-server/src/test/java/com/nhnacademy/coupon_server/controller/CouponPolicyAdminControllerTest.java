package com.nhnacademy.coupon_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.coupon_server.dto.request.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CouponPolicyAdminController.class,
        properties = {"spring.cloud.config.enabled=false"}
)
@DisplayName("CouponPolicyAdminController 컨트롤러 테스트")
class CouponPolicyAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponPolicyService couponPolicyService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("쿠폰 정책 생성 성공")
    void createCouponPolicySuccess() throws Exception {
        CouponPolicyRequestDto requestDto = CouponPolicyRequestDto.builder()
                .name("신규 가입 쿠폰")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(10000L)
                .maxDiscountValue(1000L)
                .build();

        CouponPolicyResponseDto responseDto = CouponPolicyResponseDto.builder()
                .id(1L)
                .name("신규 가입 쿠폰")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(10000L)
                .maxDiscountValue(1000L)
                .build();

        when(couponPolicyService.create(any(CouponPolicyRequestDto.class))).thenReturn(responseDto)
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/coupons/admin/coupon-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("신규 가입 쿠폰"))
                .andExpect(jsonPath("$.discountType").value("FIXED"))
                .andExpect(jsonPath("$.discountValue").value(1000L));
    }

    @Test
    @DisplayName("쿠폰 정책 생성 실패 - 유효성 검사 실패 (이름 누락)")
    void createCouponPolicyFailureNoName() throws Exception {
        CouponPolicyRequestDto requestDto = CouponPolicyRequestDto.builder()
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(10000L)
                .maxDiscountValue(5000L)
                .build();

        mockMvc.perform(post("/api/coupons/admin/coupon-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("쿠폰 정책 생성 실패 - 유효성 검사 실패 (할인 금액 음수)")
    void createCouponPolicyFailureNegativeDiscount() throws Exception {
        CouponPolicyRequestDto requestDto = CouponPolicyRequestDto.builder()
                .name("할인 금액 오류 정책")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(-1000L)
                .build();

        mockMvc.perform(post("/api/coupons/admin/coupon-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("모든 쿠폰 정책 조회 성공")
    void getAllPolicies_success() throws Exception {
        CouponPolicyResponseDto policy1 = CouponPolicyResponseDto.builder()
                .id(1L)
                .name("정책1")
                .comment(Comment.EVENT)
                .build();

        CouponPolicyResponseDto policy2 = CouponPolicyResponseDto.builder()
                .id(2L)
                .name("정책2")
                .comment(Comment.BIRTHDAY)
                .build();

        when(couponPolicyService.findAll())
                .thenReturn(List.of(policy1, policy2));

        mockMvc.perform(get("/api/coupons/admin/coupon-policies")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].name").value("정책1"));
    }

    @Test
    @DisplayName("쿠폰 정책 조회 실패 - 서버 내부 오류 발생")
    void getAllPoliciesFailureServerError() throws Exception {
        when(couponPolicyService.findAll()).thenThrow(new RuntimeException("DB 연결 실패 등 예상치 못한 오류"));
        mockMvc.perform(get("/api/coupons/admin/coupon-policies")
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 성공")
    void getPolicyById_success() throws Exception {
        Long id = 1L;
        CouponPolicyResponseDto responseDto = CouponPolicyResponseDto.builder()
                .id(id)
                .name("단건 조회 정책")
                .comment(Comment.EVENT)
                .build();

        when(couponPolicyService.findById(id)).thenReturn(responseDto);

        mockMvc.perform(get("/api/coupons/admin/coupon-policies/{id}", id)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("단건 조회 정책"));
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 실패 - 존재하지 않는 ID")
    void getPolicyByIdFailureNotFound() throws Exception {
        Long id = 999L;
        when(couponPolicyService.findById(id)).thenThrow(new CouponPolicyNotFoundException());

        mockMvc.perform(get("/api/coupons/admin/coupon-policies/{id}", id)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("쿠폰 정책 비활성화 성공")
    void deletePoliciesSuccess() throws Exception {
        Long id = 1L;
        doNothing().when(couponPolicyService).deleteById(id);

        mockMvc.perform(delete("/api/coupons/admin/coupon-policies/{id}", id))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("쿠폰 정책 비활성화 실패 - 존재하지 않는 정책")
    void deletePoliciesFailure() throws Exception {
        Long id = 999L;
        doThrow(new CouponPolicyNotFoundException()).when(couponPolicyService).deleteById(id);
        mockMvc.perform(delete("/api/coupons/admin/coupon-policies/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유효성 검사 실패 (400)")
    void handleValidationExceptionTest() throws Exception {
        CouponPolicyRequestDto requestDto = CouponPolicyRequestDto.builder()
                .name("")
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();

        mockMvc.perform(post("/api/coupons/admin/coupon-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("서버 내부 오류 (500)")
    void handleAllExceptionTest() throws Exception {
        when(couponPolicyService.findAll())
                .thenThrow(new RuntimeException("DB 연결 끊김 등 심각한 오류"));

        mockMvc.perform(get("/api/coupons/admin/coupon-policies")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    @Test
    @DisplayName("쿠폰 정책 찾기 실패 (404)")
    void handleNotFoundExceptionTest() throws Exception {
        Long id = 999L;

        doThrow(new CouponPolicyNotFoundException())
                .when(couponPolicyService).deleteById(id);

        mockMvc.perform(delete("/api/coupons/admin/coupon-policies/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COUPON_POLICY_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.COUPON_POLICY_NOT_FOUND.getMessage()));
    }
}
