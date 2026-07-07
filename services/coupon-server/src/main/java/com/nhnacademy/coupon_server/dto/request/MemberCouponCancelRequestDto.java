package com.nhnacademy.coupon_server.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberCouponCancelRequestDto {
    @NotNull(message = "쿠폰 ID는 필수입니다.")
    private Long couponId;

    @NotNull(message = "주문 ID는 필수입니다.")
    private Long orderId;
}
