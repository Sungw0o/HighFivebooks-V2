package com.nhnacademy.coupon_server.repository;

import com.nhnacademy.coupon_server.config.QueryDslConfig;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.CouponPolicyBook;
import com.nhnacademy.coupon_server.entity.CouponPolicyCategory;
import com.nhnacademy.coupon_server.entity.state.*;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CouponPolicy couponPolicy;

    @BeforeEach
    void setUp() {
        couponPolicy = CouponPolicy.builder()
                .name("테스트 정책")
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .maxDiscountValue(1000L)
                .build();
        entityManager.persist(couponPolicy);
    }

    @Test
    @DisplayName("현재 발급 가능한 기간의 쿠폰만 조회한다")
    void findAllByIssuedStartAtBeforeAndIssuedEndAtAfter() {
        LocalDateTime now = LocalDateTime.now();
        Coupon activeCoupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("발급 가능 쿠폰")
                .issueCount(100)
                .issuedStartAt(now.minusDays(1))
                .issuedEndAt(now.plusDays(1))
                .validPeriodDate(30)
                .build();
        Coupon futureCoupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("미래 쿠폰")
                .issueCount(100)
                .issuedStartAt(now.plusDays(1))
                .issuedEndAt(now.plusDays(2))
                .validPeriodDate(30)
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .couponPolicy(couponPolicy)
                .couponName("만료된 쿠폰")
                .issueCount(100)
                .issuedStartAt(now.minusDays(2))
                .issuedEndAt(now.minusDays(1))
                .validPeriodDate(30)
                .build();

        entityManager.persist(activeCoupon);
        entityManager.persist(futureCoupon);
        entityManager.persist(expiredCoupon);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Coupon> result = couponRepository.findIssuableCoupons(now, CouponPolicyStatus.ACTIVE, CouponType.NORMAL, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCouponName()).isEqualTo("발급 가능 쿠폰");
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 조회 - 날짜 조건(Null 포함) 및 상태 확인")
    void findIssuableCoupons_WithDateAndStatus() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 정상 기간
        createCoupon("정상 쿠폰", couponPolicy, now.minusDays(1), now.plusDays(1));
        // 2. 시작일 null (무제한 시작)
        createCoupon("시작일 무제한", couponPolicy, null, now.plusDays(1));
        // 3. 종료일 null (무제한 종료)
        createCoupon("종료일 무제한", couponPolicy, now.minusDays(1), null);
        // 4. 기간 만료 (조회 안됨)
        createCoupon("만료된 쿠폰", couponPolicy, now.minusDays(5), now.minusDays(1));
        // 5. 미래 시작 (조회 안됨)
        createCoupon("미래 쿠폰", couponPolicy, now.plusDays(1), now.plusDays(5));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Coupon> result = couponRepository.findIssuableCoupons(now, CouponPolicyStatus.ACTIVE, CouponType.NORMAL, pageable);

        assertThat(result.getContent()).hasSize(3)
                .extracting("couponName")
                .containsExactlyInAnyOrder("정상 쿠폰", "시작일 무제한", "종료일 무제한");
    }

    @Test
    @DisplayName("상품 상세 페이지 쿠폰 조회 - 타겟 도서, 타겟 카테고리, 전역 정책 포함")
    void findCouponsForProduct_IncludesGlobalAndTargets() {
        LocalDateTime now = LocalDateTime.now();
        Long targetBookId = 100L;
        Long otherBookId = 999L;
        Long targetCategoryId = 10L;
        Long otherCategoryId = 99L;

        // 1. [매칭] 도서 지정 정책 & 쿠폰
        CouponPolicy bookPolicy = createPolicy("도서 정책");
        entityManager.persist(CouponPolicyBook.builder().couponPolicy(bookPolicy).bookId(targetBookId).build());
        createCoupon("도서 쿠폰", bookPolicy, null, null);

        // 2. [매칭] 카테고리 지정 정책 & 쿠폰
        CouponPolicy categoryPolicy = createPolicy("카테고리 정책");
        entityManager.persist(CouponPolicyCategory.builder().couponPolicy(categoryPolicy).categoryId(targetCategoryId).build());
        createCoupon("카테고리 쿠폰", categoryPolicy, null, null);

        // 3. [매칭] 전역 정책 (조건 없음) & 쿠폰
        CouponPolicy globalPolicy = createPolicy("전역 정책");
        createCoupon("전역 쿠폰", globalPolicy, null, null);

        // 4. [제외] 다른 도서 정책
        CouponPolicy otherBookPolicy = createPolicy("다른 도서 정책");
        entityManager.persist(CouponPolicyBook.builder().couponPolicy(otherBookPolicy).bookId(otherBookId).build());
        createCoupon("다른 도서 쿠폰", otherBookPolicy, null, null);

        // 5. [제외] 다른 카테고리 정책
        CouponPolicy otherCategoryPolicy = createPolicy("다른 카테고리 정책");
        entityManager.persist(CouponPolicyCategory.builder().couponPolicy(otherCategoryPolicy).categoryId(otherCategoryId).build());
        createCoupon("다른 카테고리 쿠폰", otherCategoryPolicy, null, null);

        entityManager.flush();
        entityManager.clear();

        List<Coupon> result = couponRepository.findCouponsForProduct(targetBookId, List.of(targetCategoryId), CouponPolicyStatus.ACTIVE, now);

        assertThat(result).hasSize(3)
                .extracting("couponName")
                .containsExactlyInAnyOrder("도서 쿠폰", "카테고리 쿠폰", "전역 쿠폰");
    }

    @Test
    @DisplayName("상품 전용 쿠폰 조회 - 전역 정책 제외, 오직 타겟 도서/카테고리만")
    void findSpecificCouponsForProduct_ExcludesGlobal() {
        LocalDateTime now = LocalDateTime.now();
        Long targetBookId = 100L;
        Long targetCategoryId = 10L;

        // 1. [매칭] 도서 지정
        CouponPolicy bookPolicy = createPolicy("도서 정책");
        entityManager.persist(CouponPolicyBook.builder().couponPolicy(bookPolicy).bookId(targetBookId).build());
        createCoupon("도서 쿠폰", bookPolicy, null, null);

        // 2. [매칭] 카테고리 지정
        CouponPolicy categoryPolicy = createPolicy("카테고리 정책");
        entityManager.persist(CouponPolicyCategory.builder().couponPolicy(categoryPolicy).categoryId(targetCategoryId).build());
        createCoupon("카테고리 쿠폰", categoryPolicy, null, null);

        // 3. [제외] 전역 정책 (Specific 조회에서는 제외되어야 함)
        CouponPolicy globalPolicy = createPolicy("전역 정책");
        createCoupon("전역 쿠폰", globalPolicy, null, null);

        entityManager.flush();
        entityManager.clear();

        List<Coupon> result = couponRepository.findSpecificCouponsForProduct(targetBookId, List.of(targetCategoryId), CouponPolicyStatus.ACTIVE, now);

        assertThat(result).hasSize(2)
                .extracting("couponName")
                .containsExactlyInAnyOrder("도서 쿠폰", "카테고리 쿠폰");
    }

    @Test
    @DisplayName("코멘트(용도) 및 정책 상태별 조회")
    void findCouponsByCommentAndStatus() {
        // 1. [매칭] WELCOME & ACTIVE
        CouponPolicy welcomePolicy = createPolicyWithComment("웰컴 정책", Comment.WELCOME, CouponPolicyStatus.ACTIVE);
        createCoupon("웰컴 쿠폰", welcomePolicy, null, null);

        // 2. [제외] BIRTHDAY & ACTIVE
        CouponPolicy birthdayPolicy = createPolicyWithComment("생일 정책", Comment.BIRTHDAY, CouponPolicyStatus.ACTIVE);
        createCoupon("생일 쿠폰", birthdayPolicy, null, null);

        // 3. [제외] WELCOME & INACTIVE
        CouponPolicy inactiveWelcomePolicy = createPolicyWithComment("비활성 웰컴 정책", Comment.WELCOME, CouponPolicyStatus.INACTIVE);
        createCoupon("비활성 쿠폰", inactiveWelcomePolicy, null, null);

        Pageable pageable = PageRequest.of(0, 10);
        List<Coupon> result = couponRepository.findCouponsByCommentAndStatus(Comment.WELCOME, CouponPolicyStatus.ACTIVE, pageable);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCouponName()).isEqualTo("웰컴 쿠폰");
    }

    // --- Helper Methods ---

    private void createCoupon(String name, CouponPolicy policy, LocalDateTime start, LocalDateTime end) {
        Coupon coupon = Coupon.builder()
                .couponPolicy(policy)
                .couponName(name)
                .issueCount(100)
                .issuedStartAt(start)
                .issuedEndAt(end)
                .validPeriodDate(30)
                .status(CouponStatus.ACTIVE)
                .couponType(CouponType.NORMAL)
                .build();
        entityManager.persist(coupon);
    }

    private CouponPolicy createPolicy(String name) {
        CouponPolicy policy = CouponPolicy.builder()
                .name(name)
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .maxDiscountValue(1000L)
                .status(CouponPolicyStatus.ACTIVE)
                .build();
        entityManager.persist(policy);
        return policy;
    }

    private CouponPolicy createPolicyWithComment(String name, Comment comment, CouponPolicyStatus status) {
        CouponPolicy policy = CouponPolicy.builder()
                .name(name)
                .comment(comment)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .status(status)
                .build();
        entityManager.persist(policy);
        return policy;
    }
}