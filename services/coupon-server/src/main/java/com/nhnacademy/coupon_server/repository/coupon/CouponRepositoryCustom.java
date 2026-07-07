package com.nhnacademy.coupon_server.repository.coupon;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface CouponRepositoryCustom {

    // 발급 가능한 쿠폰 페이징 조회
    Page<Coupon> findIssuableCoupons(LocalDateTime now, CouponPolicyStatus status, CouponType couponType, Pageable pageable);

    // 상품(도서) 상세 페이지용 쿠폰 조회 (범용 포함 여부에 따라 로직 분기 가능)
    List<Coupon> findCouponsForProduct(Long bookId, List<Long> categoryIds, CouponPolicyStatus status, LocalDateTime now);

    // 상품(도서) 전용 쿠폰만 조회 (범용 제외)
    List<Coupon> findSpecificCouponsForProduct(Long bookId, List<Long> categoryIds, CouponPolicyStatus status, LocalDateTime now);

    // 특정 Comment와 상태를 가진 쿠폰 조회
    List<Coupon> findCouponsByCommentAndStatus(Comment comment, CouponPolicyStatus status, Pageable pageable);
}
