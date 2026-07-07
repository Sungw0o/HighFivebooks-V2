package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MemberCouponService {
    Page<MemberCouponResponseDto> findAll(Pageable pageable);

    void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto);

    void issueCouponByUser(Long userId, Long couponId);

    Page<MemberCouponResponseDto> findCouponByUserId(Long userId, Pageable pageable);

    List<MemberCouponResponseDto> findUsableCoupons(Long userId, List<Long> bookIds, List<Long> categoryIds);

    CouponCalculationResponseDto calculateDiscount(Long userId, CouponCalculationRequestDto requestDto);

    void useCoupon(Long userId, MemberCouponUseRequestDto requestDto);

    void cancelCouponUsage(Long userId, MemberCouponCancelRequestDto requestDto);

    void issueBirthdayCoupon(Long userId, Long couponId);

    void issueWelcomeCoupon(Long memberId);

    void createMemberCoupon(Long userId, Long couponId);
}
