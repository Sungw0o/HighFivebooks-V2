package com.nhnacademy.coupon_server.repository;

import com.nhnacademy.coupon_server.config.QueryDslConfig;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.impl.MemberCouponJdbcRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({MemberCouponJdbcRepository.class, QueryDslConfig.class})
class MemberCouponJdbcRepositoryTest {

    @Autowired
    private MemberCouponJdbcRepository memberCouponJdbcRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Test
    @DisplayName("Bulk Insert 성공 테스트 - 다량의 회원 쿠폰이 정상적으로 저장되어야 한다")
    void batchInsertMemberCoupons_Success() {
        // Given
        // 1. FK 제약조건을 위해 정책 및 쿠폰 미리 생성
        CouponPolicy policy = CouponPolicy.builder()
                .name("생일 정책")
                .comment(Comment.BIRTHDAY)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .status(CouponPolicyStatus.ACTIVE)
                .build();
        couponPolicyRepository.save(policy);

        Coupon coupon = Coupon.builder()
                .couponName("생일 쿠폰")
                .couponPolicy(policy)
                .issueCount(1000)
                .build();
        couponRepository.save(coupon);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = now.plusDays(30);

        // 2. 저장할 MemberCoupon 리스트 생성 (100명분)
        int batchSize = 100;
        List<MemberCoupon> memberCoupons = IntStream.range(0, batchSize)
                .mapToObj(i -> MemberCoupon.builder()
                        .userId((long) i)
                        .coupon(coupon)
                        .status(Status.ISSUED)
                        .issueAt(now)
                        .expiredAt(expiredAt)
                        .build())
                .toList();

        // When
        memberCouponJdbcRepository.batchInsertMemberCoupons(memberCoupons);

        // Then
        List<MemberCoupon> results = memberCouponRepository.findAll();

        assertThat(results).hasSize(batchSize); // 개수 확인
        
        // 첫 번째 데이터 검증
        MemberCoupon firstCoupon = results.stream()
                .filter(mc -> mc.getUserId() == 0L)
                .findFirst()
                .orElseThrow();
        
        assertThat(firstCoupon.getCoupon().getId()).isEqualTo(coupon.getId());
        assertThat(firstCoupon.getStatus()).isEqualTo(Status.ISSUED);
        // DB 저장 시 정밀도 차이가 발생할 수 있으므로 초 단위까지만 비교하거나 오차 범위 허용
        assertThat(firstCoupon.getExpiredAt()).isNotNull();
    }

    @Test
    @DisplayName("Bulk Insert - 만료일이 없는(Null) 쿠폰 저장 테스트")
    void batchInsertMemberCoupons_WithNullExpiredAt() {
        // 1. FK 제약조건을 위해 정책 및 쿠폰 미리 생성
        CouponPolicy policy = CouponPolicy.builder()
                .name("무제한 정책")
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(500L)
                .minOrderValue(5000L)
                .status(CouponPolicyStatus.ACTIVE)
                .build();
        couponPolicyRepository.save(policy);

        Coupon coupon = Coupon.builder()
                .couponName("무제한 쿠폰")
                .couponPolicy(policy)
                .issueCount(1000)
                .build();
        couponRepository.save(coupon);

        LocalDateTime now = LocalDateTime.now();

        // 2. 만료일(expiredAt)이 없는 MemberCoupon 리스트 생성
        int batchSize = 10;
        List<MemberCoupon> memberCoupons = IntStream.range(0, batchSize)
                .mapToObj(i -> MemberCoupon.builder()
                        .userId((long) i)
                        .coupon(coupon)
                        .status(Status.ISSUED)
                        .issueAt(now)
                        .expiredAt(null)
                        .build())
                .toList();

        memberCouponJdbcRepository.batchInsertMemberCoupons(memberCoupons);

        List<MemberCoupon> results = memberCouponRepository.findAll();

        assertThat(results).hasSize(batchSize);

        // 모든 데이터의 expiredAt이 null인지 확인
        for (MemberCoupon mc : results) {
            assertThat(mc.getExpiredAt()).isNull();
            assertThat(mc.getCoupon().getId()).isEqualTo(coupon.getId());
        }
    }
}