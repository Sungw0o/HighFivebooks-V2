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
public class UserCouponIssueRequestDto {

    @NotNull(message = "발급받을 쿠폰 ID는 필수입니다.")
    private Long couponId;
}
