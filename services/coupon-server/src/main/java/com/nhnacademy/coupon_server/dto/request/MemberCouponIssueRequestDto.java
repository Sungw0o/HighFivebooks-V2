package com.nhnacademy.coupon_server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberCouponIssueRequestDto {

    @NotNull(message = "쿠폰 템플릿 ID는 필수입니다.")
    private Long couponId;

    @NotNull(message = "발급 대상 회원 ID는 필수입니다.")
    private Long userId;
}
