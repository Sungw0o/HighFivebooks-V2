package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberCouponServiceImpl implements MemberCouponService {
    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponDateCalculator dateCalculator;
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> issueScript = issueCouponScript();

    @Override
    public Page<MemberCouponResponseDto> findAll(Pageable pageable) {
        return memberCouponRepository.findAll(pageable).map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    @Transactional
    public void createMemberCoupon(Long userId, Long couponId) {
        log.info("DB 저장 시작 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw new DuplicateCouponException();
        }

        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(dateCalculator.calculateExpiration(coupon))
                .build();

        memberCouponRepository.save(memberCoupon);
    }

    @Override
    @Transactional
    public void issueCouponByAdmin(MemberCouponIssueRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        Long couponId = requestDto.getCouponId();

        log.info("관리자 수동 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        Coupon coupon = getCouponOrThrow(couponId);

        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 발급할 수 없습니다.");
        }
        validateDuplicateAndSave(userId, coupon);
    }

    @Override
    @Transactional
    public void issueCouponByUser(Long userId, Long couponId) {
        log.info("사용자 쿠폰 발급 요청 - Coupon: {}, User: {}", couponId, userId);

        // 쿠폰 정보 조회 (없으면 예외)
        Coupon coupon = getCouponOrThrow(couponId);

        // 발급 가능 기간 및 상태 검증
        validateCouponIssuance(coupon);

        // 선착순/수량 제한 쿠폰인 경우 Redis를 통한 동시성 제어 수행
        if (coupon.getIssueCount() != null) {
            String countKey = "coupon:count:" + couponId;
            String issuedUsersKey = "coupon:issued:" + couponId + ":users";

            // 2. Lua Script 실행 (원자적 처리)
            Long result = redisTemplate.execute(issueScript,
                    List.of(countKey, issuedUsersKey),
                    String.valueOf(userId));

            if (result == null || result == 0) {
                throw new IllegalStateException("쿠폰이 모두 소진되었습니다.");
            }
            if (result == -1) {
                // 혹시 모를 Redis-DB 데이터 불일치 확인 (DB에도 있는지 더블 체크)
                if (memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
                    throw new DuplicateCouponException();
                }
                log.warn("Redis 불일치 감지: Redis 발급 이력 있음 / DB 없음 -> 재저장 시도 (User: {}, Coupon: {})", userId, couponId);
            }
        }
        // Redis 통과 후 DB에 최종 저장 (실패 시 트랜잭션 롤백됨)
        saveMemberCoupon(userId, coupon);
    }

    @Override
    public Page<MemberCouponResponseDto> findCouponByUserId(Long userId, Pageable pageable) {
        return memberCouponRepository.findMemberCouponsByUserId(userId, pageable)
                .map(MemberCouponResponseDto::fromEntity);
    }

    @Override
    public List<MemberCouponResponseDto> findUsableCoupons(Long userId, List<Long> bookIds, List<Long> categoryIds) {
        List<MemberCoupon> allCoupons = memberCouponRepository.findUsableCoupons(userId, LocalDateTime.now());
        if (bookIds == null || bookIds.isEmpty()) {
            return allCoupons.stream()
                    .map(MemberCouponResponseDto::fromEntity)
                    .toList();
        }
        return allCoupons.stream()
                .filter(memberCoupon -> isApplicableToBooks(memberCoupon.getCoupon(), bookIds, categoryIds))
                .map(MemberCouponResponseDto::fromEntity)
                .toList();
    }

    private boolean isApplicableToBooks(Coupon coupon, List<Long> bookIds, List<Long> categoryIds) {
        CouponPolicy policy = coupon.getCouponPolicy();
        boolean hasBookConstraint = !policy.getUsableBooks().isEmpty();
        boolean hasCategoryConstraint = !policy.getUsableCategories().isEmpty();

        if (hasBookConstraint) {
            boolean match = policy.getUsableBooks().stream()
                    .anyMatch(policyBook -> bookIds.contains(policyBook.getBookId()));
            if (match) return true;
        }

        if (hasCategoryConstraint && categoryIds != null && !categoryIds.isEmpty()) {
            boolean match = policy.getUsableCategories().stream()
                    .anyMatch(policyCategory -> categoryIds.contains(policyCategory.getCategoryId()));
            if (match) return true;
        }

        return !hasCategoryConstraint && !hasBookConstraint;
    }

    @Override
    public CouponCalculationResponseDto calculateDiscount(Long userId, CouponCalculationRequestDto requestDto) {
        MemberCoupon memberCoupon = findAndValidateOwner(requestDto.getCouponId(), userId);

        memberCoupon.validateUsable();

        long discountAmount = memberCoupon.getCoupon().getCouponPolicy().calculateDiscountAmount(requestDto.getTotalOrderPrice());

        if (discountAmount > requestDto.getTotalOrderPrice()) {
            log.warn("할인 금액이 주문 금액 초과 - MemberCoupon: {}, 주문금액: {}, 할인금액: {}",
                    requestDto.getCouponId(), requestDto.getTotalOrderPrice(), discountAmount);
            discountAmount = requestDto.getTotalOrderPrice();
        }

        return CouponCalculationResponseDto.builder()
                .discountAmount(discountAmount)
                .finalPrice(requestDto.getTotalOrderPrice() - discountAmount)
                .build();
    }

    // [수정됨] 쿠폰 사용 처리
    @Override
    @Transactional
    public void useCoupon(Long userId, MemberCouponUseRequestDto requestDto) {
        // 쿠폰 소유자 검증 및 조회
       MemberCoupon memberCoupon = findAndValidateOwner(requestDto.getCouponId(), userId);
        // 쿠폰 사용 상태로 변경 (Dirty Checking으로 DB 업데이트)
       memberCoupon.use(requestDto.getOrderId());
    }

    // [수정됨] 쿠폰 사용 취소
    @Override
    @Transactional
    public void cancelCouponUsage(Long userId, MemberCouponCancelRequestDto requestDto) {
        MemberCoupon memberCoupon = findAndValidateOwner(requestDto.getCouponId(), userId);

        memberCoupon.cancel();
    }

    private  Coupon getCouponOrThrow(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);
    }

    private void validateDuplicateAndSave(Long userId, Coupon coupon) {
        if (memberCouponRepository.existsByUserIdAndCouponId(userId, coupon.getId())) {
            throw new DuplicateCouponException();
        }
        try {
            saveMemberCoupon(userId, coupon);
        }catch (DataIntegrityViolationException e) {
            throw new DuplicateCouponException();
        }
    }

    private void saveMemberCoupon(Long userId, Coupon coupon) {
        MemberCoupon memberCoupon = MemberCoupon.builder()
                .coupon(coupon)
                .userId(userId)
                .status(Status.ISSUED)
                .issueAt(LocalDateTime.now())
                .expiredAt(dateCalculator.calculateExpiration(coupon))
                .build();
        memberCouponRepository.save(memberCoupon);
    }

    private MemberCoupon findAndValidateOwner(Long memberCouponId, Long userId) {
        MemberCoupon memberCoupon = memberCouponRepository.findById(memberCouponId)
                .orElseThrow(CouponNotFoundException::new);
        memberCoupon.validateOwner(userId);
        return memberCoupon;
    }

    private void validateCouponIssuance(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getIssuedStartAt() != null && now.isBefore(coupon.getIssuedStartAt())) {
            throw new IllegalArgumentException("아직 발급 가능한 기간이 아닙니다.");
        }
        if (coupon.getIssuedEndAt() != null && now.isAfter(coupon.getIssuedEndAt())) {
            throw new IllegalArgumentException("발급 기간이 지났습니다.");
        }
        if (coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            throw new IllegalStateException("해당 쿠폰의 정책이 중단되어 더 이상 발급받을 수 없습니다.");
        }
    }

    @Override
    @Transactional
    public void issueBirthdayCoupon(Long memberId, Long couponId) {
        log.info("생일 쿠폰 발급 요청 - User: {}, Coupon: {}", memberId, couponId);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(CouponNotFoundException::new);

        if (memberCouponRepository.existsByUserIdAndCouponId(memberId, couponId)) {
            log.warn("이미 생일 쿠폰을 발급받은 회원입니다. User: {}", memberId);
            return;
        }

        saveMemberCoupon(memberId, coupon);
    }

    @Override
    @Transactional
    public void issueWelcomeCoupon(Long memberId) {
        log.info("웰컴 쿠폰 자동 지급 시도 - User: {}", memberId);
        List<Coupon> coupons = couponRepository.findCouponsByCommentAndStatus(
                Comment.WELCOME,
                CouponPolicyStatus.ACTIVE,
                PageRequest.of(0, 1)
        );

        if (coupons.isEmpty()) {
            // 웰컴 쿠폰 정책이 없으면 예외 던짐 (또는 조용히 로그만 남기고 종료)
            throw new CouponNotFoundException();
        }
        Coupon welcomeCoupon = coupons.get(0);

        if (memberCouponRepository.existsByUserIdAndCouponId(memberId, welcomeCoupon.getId())) {
            log.info("이미 웰컴 쿠폰을 받은 회원입니다. User: {}", memberId);
            return;
        }

        saveMemberCoupon(memberId, welcomeCoupon);
    }

    private RedisScript<Long> issueCouponScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/issue-coupon.lua")));
        script.setResultType(Long.class);
        return script;
    }
}