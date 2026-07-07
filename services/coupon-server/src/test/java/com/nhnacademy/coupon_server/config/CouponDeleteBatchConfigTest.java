package com.nhnacademy.coupon_server.config;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(properties = "spring.batch.job.enabled=false")
class CouponDeleteBatchConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponPolicyRepository couponPolicyRepository;

    @Autowired
    @Qualifier("deleteExpiredCouponJob")
    private Job deleteExpiredCouponJob;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(deleteExpiredCouponJob);
    }

    @AfterEach
    void tearDown() {
        memberCouponRepository.deleteAll();
        couponRepository.deleteAll();
        couponPolicyRepository.deleteAll();
    }

    @Test
    @DisplayName("만료되었거나 사용된 쿠폰만 삭제되고, 정상 쿠폰은 유지되어야 한다")
    void deleteExpiredCouponJobSuccess() throws Exception {
        CouponPolicy policy = CouponPolicy.builder()
                .name("테스트 정책")
                .comment(Comment.EVENT)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .status(CouponPolicyStatus.ACTIVE)
                .build();
        couponPolicyRepository.save(policy);

        Coupon coupon = Coupon.builder()
                .couponName("테스트 쿠폰")
                .couponPolicy(policy)
                .build();
        couponRepository.save(coupon);

        LocalDateTime now = LocalDateTime.now();

        MemberCoupon usedCoupon = MemberCoupon.builder()
                .userId(1L)
                .coupon(coupon)
                .status(Status.USED)
                .issueAt(now.minusDays(5))
                .expiredAt(now.plusDays(5)) // 기간 남았어도 사용했으면 삭제
                .build();

        // Case B: [삭제 대상] 만료됨 (ISSUED, 기간 지남)
        MemberCoupon expiredCoupon = MemberCoupon.builder()
                .userId(2L)
                .coupon(coupon)
                .status(Status.ISSUED)
                .issueAt(now.minusDays(10))
                .expiredAt(now.minusDays(1)) // 어제 만료됨
                .build();

        // Case C: [유지 대상] 정상 (ISSUED, 기간 남음)
        MemberCoupon validCoupon = MemberCoupon.builder()
                .userId(3L)
                .coupon(coupon)
                .status(Status.ISSUED)
                .issueAt(now.minusDays(1))
                .expiredAt(now.plusDays(10)) // 10일 뒤 만료
                .build();

        memberCouponRepository.saveAll(List.of(usedCoupon, expiredCoupon, validCoupon));

        // 배치 Job 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // 1. Job이 정상 종료되었는지 확인
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 2. 데이터 검증
        List<MemberCoupon> remainingCoupons = memberCouponRepository.findAll();

        // 총 3개 중 2개 삭제 -> 1개만 남아야 함
        assertThat(remainingCoupons).hasSize(1);

        // 남은 1개는 validCoupon이어야 함
        MemberCoupon survivor = remainingCoupons.get(0);
        assertThat(survivor.getUserId()).isEqualTo(3L);
        assertThat(survivor.getStatus()).isEqualTo(Status.ISSUED);
        assertThat(survivor.getExpiredAt()).isAfter(now);
    }
}