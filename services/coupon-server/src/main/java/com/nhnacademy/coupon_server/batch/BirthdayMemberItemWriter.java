package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.impl.MemberCouponJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class BirthdayMemberItemWriter implements ItemWriter<Long> {

    private final MemberCouponRepository memberCouponRepository;
    private final MemberCouponJdbcRepository memberCouponJdbcRepository;
    private final CouponRepository couponRepository;
    private final CouponDateCalculator dateCalculator;

    private Coupon cachedBirthdayCoupon;

    // Step 실행 전에 한 번 실행되는 메서드
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        // 생일 쿠폰 정책을 DB에서 조회하여 캐싱
        this.cachedBirthdayCoupon = fetchBirthdayCoupon();
        // 정책이 없으면 배치를 진행할 수 없으므로 예외 발생
        if (this.cachedBirthdayCoupon == null) {
            throw new IllegalStateException("활성화된 생일 쿠폰 정책을 찾을 수 없습니다.");
        }
    }

    // 실제 쓰기 작업 (Chunk 단위로 실행됨)
    @Override
    public void write(Chunk<? extends Long> chunk) {
        // Chunk에 있는 유저 ID 리스트 가져오기
        List<Long> userIds = new ArrayList<>(chunk.getItems());

        if (userIds.isEmpty()) return;

        // 1. [조회 최적화] 이미 발급받은 유저 ID 조회 (IN 절, 1회 쿼리)
        // Loop를 돌며 확인하면 성능 저하가 발생하므로 한 번에 조회함
        List<Long> alreadyIssuedIds = memberCouponRepository.findUserIdsByCouponIdAndUserIdIn(
                cachedBirthdayCoupon.getId(),
                userIds
        );

        // 2. 중복 제거 (발급 대상만 필터링)
        List<Long> targetUserIds = userIds.stream()
                .filter(id -> !alreadyIssuedIds.contains(id))
                .toList();

        // 발급 대상이 없으면 종료
        if (targetUserIds.isEmpty()) {
            log.info("이번 청크는 모두 이미 발급된 유저입니다. (Skip)");
            return;
        }

        // 3. Entity 리스트 생성 (메모리 작업)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredAt = dateCalculator.calculateExpiration(cachedBirthdayCoupon); // 배치 내 동일 만료일 적용

        List<MemberCoupon> memberCoupons = targetUserIds.stream()
                .map(userId -> MemberCoupon.builder()
                        .coupon(cachedBirthdayCoupon)
                        .userId(userId)
                        .status(Status.ISSUED)
                        .issueAt(now)
                        .expiredAt(expiredAt)
                        .build())
                .toList();

        // 4. [쓰기 최적화] JDBC Template을 이용한 Bulk Insert 수행 (쿼리 1번으로 수천 건 저장)
        memberCouponJdbcRepository.batchInsertMemberCoupons(memberCoupons);

        log.info("Bulk Insert 완료: {}명 (중복 제외됨)", memberCoupons.size());
    }
    // 생일 쿠폰 정책 조회 헬퍼 메서드
    private Coupon fetchBirthdayCoupon() {
        // DB에서 COMMENT가 BIRTHDAY이고 상태가 ACTIVE인 쿠폰 조회 (최신순 1개)
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(
                Comment.BIRTHDAY,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "id"))
        );
        return coupons.isEmpty() ? null : coupons.get(0);
    }
}
