package com.nhnacademy.coupon_server.dto.request;

import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CouponStatusRequestDto {
    @NotNull(message = "상태 값은 필수입니다.")
    private CouponStatus status;
}