package com.nhnacademy.coupon_server.repository.membercoupon;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long>, MemberCouponRepositoryCustom {
    // 중복 발급 확인
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    // 해당 쿠폰이 현재까지 몇개 발급되었는지 카운트
    long countByCouponId(Long couponId);

    @EntityGraph(attributePaths = {"coupon", "coupon.couponPolicy"})
    Optional<MemberCoupon> findByUserIdAndCouponId(Long userId, Long couponId);

    List<MemberCoupon> findAllByCouponCouponPolicyIdAndStatus(Long policyId, Status status);

    @Query("SELECT mc.userId FROM MemberCoupon mc WHERE mc.coupon.id = :couponId AND mc.userId IN :userIds")
    List<Long> findUserIdsByCouponIdAndUserIdIn(@Param("couponId") Long couponId, @Param("userIds") List<Long> userIds);

}
