package com.nhnacademy.coupon_server.service.impl;

import com.nhnacademy.coupon_server.dto.request.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.CouponPolicyBook;
import com.nhnacademy.coupon_server.entity.CouponPolicyCategory;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyBookRepository;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyCategoryRepository;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.CouponPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponPolicyServiceImpl implements CouponPolicyService {
    private final CouponPolicyRepository couponPolicyRepository;
    private final CouponPolicyBookRepository couponPolicyBookRepository;
    private final CouponPolicyCategoryRepository couponPolicyCategoryRepository;
    private final MemberCouponRepository memberCouponRepository;

    @Override
    @Transactional
    public CouponPolicyResponseDto create(CouponPolicyRequestDto couponPolicyRequestDto) {
        log.info("새로 만든 쿠폰 정책 -> {}", couponPolicyRequestDto.getName());
        if (!couponPolicyRequestDto.isValidPercentage()) {
            throw new IllegalArgumentException("정률 할인은 100%를 초과할 수 없습니다.");
        }

        Long maxDiscountValue = couponPolicyRequestDto.getMaxDiscountValue();

        if (maxDiscountValue == null || maxDiscountValue == 0) {
            if (couponPolicyRequestDto.getDiscountType() == DiscountType.FIXED) {
                maxDiscountValue = couponPolicyRequestDto.getDiscountValue();
            } else if (couponPolicyRequestDto.getDiscountType() == DiscountType.PERCENTAGE) {
                maxDiscountValue = null;
            }
        }

        CouponPolicy newPolicy = CouponPolicy.builder()
                .name(couponPolicyRequestDto.getName())
                .comment(couponPolicyRequestDto.getComment())
                .discountType(couponPolicyRequestDto.getDiscountType())
                .discountValue(couponPolicyRequestDto.getDiscountValue())
                .minOrderValue(couponPolicyRequestDto.getMinOrderValue())
                .maxDiscountValue(maxDiscountValue)
                .build();

        CouponPolicy savedPolicy = couponPolicyRepository.save(newPolicy);
        // 도서 적용 범위 저장 로직
        if (couponPolicyRequestDto.getTargetBookIds() != null && !couponPolicyRequestDto.getTargetBookIds().isEmpty()) {
            List<CouponPolicyBook> bookLinks = couponPolicyRequestDto.getTargetBookIds().stream()
                    .map(bookId -> CouponPolicyBook.builder()
                            .couponPolicy(savedPolicy)
                            .bookId(bookId)
                            .build())
                    .toList();
            couponPolicyBookRepository.saveAll(bookLinks);
            savedPolicy.setUsableBooks(new HashSet<>(bookLinks));
        }
        // 카테고리 적용 범위 저장 로직
        if (couponPolicyRequestDto.getTargetCategoryIds() != null && !couponPolicyRequestDto.getTargetCategoryIds().isEmpty()) {
            List<CouponPolicyCategory> categoryLinks = couponPolicyRequestDto.getTargetCategoryIds().stream()
                    .map(categoryId -> CouponPolicyCategory.builder()
                            .couponPolicy(savedPolicy)
                            .categoryId(categoryId)
                            .build())
                    .toList();
            couponPolicyCategoryRepository.saveAll(categoryLinks);
            savedPolicy.setUsableCategories(new HashSet<>(categoryLinks));
        }
        return CouponPolicyResponseDto.fromEntity(savedPolicy);
    }

    @Override
    public List<CouponPolicyResponseDto> findAll() {
        log.info("모든 쿠폰 정책 조회 요청");

        List<CouponPolicy> couponPolicies = couponPolicyRepository.findAll();

        return couponPolicies.stream()
                .map(CouponPolicyResponseDto::fromEntity)
                .toList();
    }

    @Override
    public CouponPolicyResponseDto findById(Long id) {
        log.info("쿠폰 정책 단건 조회 요청 - ID -> {}", id);
        CouponPolicy policy = couponPolicyRepository.findById(id)
                .orElseThrow(CouponPolicyNotFoundException::new);
        return CouponPolicyResponseDto.fromEntity(policy);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("쿠폰 정책 삭제 요청 - ID -> {}", id);
        CouponPolicy policy = couponPolicyRepository.findById(id)
                .orElseThrow(CouponPolicyNotFoundException::new);

        policy.disable();
        List<MemberCoupon> issuedCoupons = memberCouponRepository.findAllByCouponCouponPolicyIdAndStatus(id, Status.ISSUED);

        for (MemberCoupon coupon : issuedCoupons) {
            coupon.setStatus(Status.EXPIRED);
        }

        log.info("정책 ID {} 관련 미사용 쿠폰 {}장 만료 처리 완료", id, issuedCoupons.size());
    }
}
