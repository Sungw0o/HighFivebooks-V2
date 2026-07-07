package com.nhnacademy.coupon_server.dto.response;

import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponResponseDto {
    private Long id;
    private Long couponPolicyId;
    private String couponName;
    private String description;
    private Integer issueCount;
    private LocalDateTime issueStartAt;
    private LocalDateTime issueEndAt;
    private Integer validPeriodDate;
    private LocalDateTime validEndAt;
    private Integer remainingCount;
    private String status;
    private String couponType;
    private String policyStatus;

    public static CouponResponseDto fromEntity(Coupon coupon) {
        return fromEntity(coupon, 0L);
    }

    public static CouponResponseDto fromEntity(Coupon coupon, long currentIssuedCount) {
        LocalDateTime now = LocalDateTime.now();
        Integer totalCount = coupon.getIssueCount();
        Integer remaining = null;

        if (totalCount != null) {
            remaining = (int) Math.max(0, totalCount - currentIssuedCount);
        }

        String status;
        if (coupon.getStatus() == CouponStatus.INACTIVE || coupon.getCouponPolicy().getStatus() == CouponPolicyStatus.INACTIVE) {
            status = "INACTIVE";
        } else if (coupon.getIssuedStartAt() != null && now.isBefore(coupon.getIssuedStartAt())) {
            status = "WAITING";
        } else if (coupon.getIssuedEndAt() != null && now.isAfter(coupon.getIssuedEndAt())) {
            status = "EXPIRED";
        } else if (remaining != null && remaining <= 0) {
            status = "SOLD_OUT";
        } else {
            status = "ACTIVE";
        }
        return CouponResponseDto.builder()
                .id(coupon.getId())
                .couponPolicyId(coupon.getCouponPolicy().getId())
                .couponName(coupon.getCouponName())
                .description(coupon.getDescription())
                .issueCount(totalCount)
                .issueStartAt(coupon.getIssuedStartAt())
                .issueEndAt(coupon.getIssuedEndAt())
                .validPeriodDate(coupon.getValidPeriodDate())
                .validEndAt(coupon.getValidEndAt())
                .remainingCount(remaining)
                .couponType(coupon.getCouponType().toString())
                .status(status)
                .policyStatus(coupon.getCouponPolicy().getStatus().name())
                .build();
    }
}
