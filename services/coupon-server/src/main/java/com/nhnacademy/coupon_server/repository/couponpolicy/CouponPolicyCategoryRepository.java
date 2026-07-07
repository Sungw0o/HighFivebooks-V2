package com.nhnacademy.coupon_server.repository.couponpolicy;

import com.nhnacademy.coupon_server.entity.CouponPolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyCategoryRepository extends JpaRepository<CouponPolicyCategory, Long> {
}
