package com.nhnacademy.coupon_server.dto.response;

import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import lombok.Builder;
import lombok.Getter;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class MemberCouponResponseDto {
    private Long id;
    private Long userId;
    private Long couponId;
    private String couponName;
    private Status status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiredAt;
    private Long orderId;
    private Long discountValue;
    private String discountType;
    private String condition;
    private Long daysRemaining;

    public static MemberCouponResponseDto fromEntity(MemberCoupon memberCoupon) {
        CouponPolicy policy = memberCoupon.getCoupon().getCouponPolicy();

        String conditionStr = "";
        if (policy.getMinOrderValue() != null && policy.getMinOrderValue() > 0) {
            DecimalFormat df = new DecimalFormat("#,###");
            conditionStr = df.format(policy.getMinOrderValue()) + "원 이상 구매 시 사용 가능";
        } else {
            conditionStr = "조건 없음";
        }
        long daysRemained = 0;
        if (memberCoupon.getExpiredAt() != null) {
            daysRemained = ChronoUnit.DAYS.between(LocalDate.now(), memberCoupon.getExpiredAt().toLocalDate());
        }
        return MemberCouponResponseDto.builder()
                .id(memberCoupon.getId())
                .userId(memberCoupon.getUserId())
                .couponId(memberCoupon.getCoupon().getId())
                .couponName(memberCoupon.getCoupon().getCouponName())
                .status(memberCoupon.getStatus())
                .issuedAt(memberCoupon.getIssueAt())
                .usedAt(memberCoupon.getUsedAt())
                .expiredAt(memberCoupon.getExpiredAt())
                .orderId(memberCoupon.getOrderId())
                .discountValue(policy.getDiscountValue())
                .discountType(policy.getDiscountType().toString())
                .condition(conditionStr)
                .daysRemaining(daysRemained)
                .build();
    }
}
