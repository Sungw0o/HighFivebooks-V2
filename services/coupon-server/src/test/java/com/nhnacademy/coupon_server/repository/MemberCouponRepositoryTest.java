package com.nhnacademy.coupon_server.repository;

import com.nhnacademy.coupon_server.config.QueryDslConfig;
import com.nhnacademy.coupon_server.dto.response.CouponCountDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class MemberCouponRepositoryTest {

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Coupon coupon;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        CouponPolicy couponPolicy = CouponPolicy.builder()
                .name("테스트 정책")
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .maxDiscountValue(1000L)
                .build();
        entityManager.persist(couponPolicy);

        coupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("테스트 쿠폰")
                .issueCount(100)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(7))
                .validPeriodDate(30)
                .build();
        entityManager.persist(coupon);
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 ID로 중복 발급 여부 확인 - 존재할 경우 True")
    void existsByUserIdAndCouponId_True() {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(30))
                .build();
        memberCouponRepository.save(memberCoupon);

        boolean exists = memberCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID와 쿠폰 ID로 중복 발급 여부 확인 - 존재하지 않을 경우 False")
    void existsByUserIdAndCouponId_False() {
        boolean exists = memberCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("특정 쿠폰의 총 발급 수 카운트")
    void countByCouponId() {
        MemberCoupon mc1 = MemberCoupon.builder()
                .coupon(coupon)
                .userId(1L)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .build();

        MemberCoupon mc2 = MemberCoupon.builder()
                .coupon(coupon)
                .userId(2L)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .build();

        memberCouponRepository.save(mc1);
        memberCouponRepository.save(mc2);

        List<CouponCountDto> results = memberCouponRepository.countByCouponIds(List.of(coupon.getId()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCouponId()).isEqualTo(coupon.getId());
        assertThat(results.get(0).getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("특정 사용자의 쿠폰 목록 조회 (페이징)")
    void findByUserId_Paging() {
        for (int i = 0; i < 10; i++) {
            Coupon newCoupon = Coupon.builder()
                    .couponPolicy(coupon.getCouponPolicy())
                    .couponName("테스트 쿠폰 " + i)
                    .issueCount(100)
                    .issuedStartAt(LocalDateTime.now().minusDays(1))
                    .issuedEndAt(LocalDateTime.now().plusDays(7))
                    .validPeriodDate(30)
                    .build();
            entityManager.persist(newCoupon);

            MemberCoupon mc = MemberCoupon.builder()
                    .coupon(newCoupon)
                    .userId(userId)
                    .status(Status.ISSUED)
                    .issueAt(LocalDateTime.now().minusHours(i))
                    .build();
            memberCouponRepository.save(mc);
        }

        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "issueAt"));

        Page<MemberCoupon> result = memberCouponRepository.findMemberCouponsByUserId(userId, pageRequest);

        assertThat(result.getContent()).hasSize(5);
        assertThat(result.getTotalElements()).isEqualTo(10);
    }

    @Test
    @DisplayName("주문 시 사용 가능한 쿠폰 조회 (상태가 ISSUED이고 만료되지 않은 쿠폰)")
    void findAllByUserIdAndStatusAndExpiredAtAfter() {
        LocalDateTime now = LocalDateTime.now();

        Coupon coupon2 = Coupon.builder()
                .couponPolicy(coupon.getCouponPolicy())
                .couponName("테스트 쿠폰 2")
                .issueCount(100)
                .build();
        entityManager.persist(coupon2);

        Coupon coupon3 = Coupon.builder()
                .couponPolicy(coupon.getCouponPolicy())
                .couponName("테스트 쿠폰 3")
                .issueCount(100)
                .build();
        entityManager.persist(coupon3);

        MemberCoupon validCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(now.minusDays(1))
                .expiredAt(now.plusDays(5))
                .build();

        MemberCoupon usedCoupon = MemberCoupon.builder()
                .coupon(coupon2)
                .userId(userId)
                .status(Status.USED)
                .issueAt(now.minusDays(5))
                .expiredAt(now.minusDays(1))
                .build();

        MemberCoupon expiredCoupon = MemberCoupon.builder()
                .coupon(coupon3)
                .userId(userId)
                .status(Status.EXPIRED)
                .issueAt(now.minusDays(10))
                .expiredAt(now.minusDays(1))
                .build();

        entityManager.persist(validCoupon);
        entityManager.persist(usedCoupon);
        entityManager.persist(expiredCoupon);

        List<MemberCoupon> result = memberCouponRepository.findUsableCoupons(userId, now);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(Status.ISSUED);
        assertThat(result.get(0).getExpiredAt()).isAfter(now);
        assertThat(result.get(0).getCoupon().getId()).isEqualTo(coupon.getId());
    }
}