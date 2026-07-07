package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.request.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CouponService {

    // 쿠폰 템플릿 생성
    CouponResponseDto create(CouponRequestDto couponRequestDto);

    // 쿠폰 템플릿 목록 조회
    List<CouponResponseDto> findAll();

    Page<CouponResponseDto> findAll(Pageable pageable);

    //발급 가능한 쿠폰 목록 조회
    Page<CouponResponseDto> findIssuableCoupons(Pageable pageable);

    void updateCouponStatus(Long couponId, CouponStatus status);

    List<CouponResponseDto> getCouponsForProduct(Long bookId, List<Long> categoryIds);

    List<CouponResponseDto> getBookSpecificCoupons(Long bookId, List<Long> categoryIds);
}
