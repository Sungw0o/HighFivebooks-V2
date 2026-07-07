package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.CouponPolicyAdminDocs;
import com.nhnacademy.coupon_server.dto.request.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/coupons/admin/coupon-policies")
public class CouponPolicyAdminController implements CouponPolicyAdminDocs {
    private final CouponPolicyService couponPolicyService;

    @Override
    public ResponseEntity<CouponPolicyResponseDto> createPolicy(@Valid @RequestBody CouponPolicyRequestDto couponPolicyRequestDto) {
        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @Override
    public ResponseEntity<List<CouponPolicyResponseDto>> getAllCouponPolicies() {
        log.info("관리자 쿠폰 정책 전체 조회 요청 수신");
        List<CouponPolicyResponseDto> policies = couponPolicyService.findAll();
        return ResponseEntity.ok(policies);
    }

    @Override
    public ResponseEntity<CouponPolicyResponseDto> getCouponPolicy(@PathVariable("couponPolicyId") Long couponPolicyId) {
        log.info("관리자 쿠폰 정책 단건 조회 요청 - ID -> {}", couponPolicyId);
        CouponPolicyResponseDto responseDto = couponPolicyService.findById(couponPolicyId);
        return ResponseEntity.ok(responseDto);
    }

    @Override
    public ResponseEntity<CouponPolicyResponseDto> deleteCouponPolicy(@PathVariable("couponPolicyId") Long couponPolicyId) {
        log.info("관리자 쿠폰 정책 비활성화 요청");
        couponPolicyService.deleteById(couponPolicyId);
        return ResponseEntity.ok().build();
    }
}
