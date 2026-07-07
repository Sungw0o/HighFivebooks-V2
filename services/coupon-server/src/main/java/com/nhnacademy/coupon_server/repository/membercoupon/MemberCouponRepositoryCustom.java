package com.nhnacademy.coupon_server.repository.membercoupon;

import com.nhnacademy.coupon_server.dto.response.CouponCountDto;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberCouponRepositoryCustom {
    // 사용 가능한 쿠폰 조회 (EntityGraph 대체)
    List<MemberCoupon> findUsableCoupons(Long userId, LocalDateTime now);

    // 쿠폰별 발급 수량 카운트 (GROUP BY 쿼리 대체)
    List<CouponCountDto> countByCouponIds(List<Long> couponIds);

    // 특정 사용자 쿠폰 페이징 조회
    Page<MemberCoupon> findMemberCouponsByUserId(Long userId, Pageable pageable);
}
