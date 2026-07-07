package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.request.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponPolicyResponseDto;

import java.util.List;

public interface CouponPolicyService {
    // 쿠폰 정책 생성
    CouponPolicyResponseDto create(CouponPolicyRequestDto couponPolicyRequestDto);

    // 쿠폰 정책 리스트 조회
    List<CouponPolicyResponseDto> findAll();

    // 쿠폰 정책 단건 조회
    CouponPolicyResponseDto findById(Long id);

    // 쿠폰 정책 삭제
    void deleteById(Long id);
}
