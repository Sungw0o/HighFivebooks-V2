package com.nhnacademy.coupon_server.dto.request;

import com.nhnacademy.coupon_server.entity.state.CouponType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponRequestDto {
    @NotNull(message = "쿠폰 정책 ID가 없습니다.")
    private Long id;

    @NotNull(message = "쿠폰 이름은 필수입니다.")
    private String couponName;

    private CouponType couponType;

    private String description;

    @PositiveOrZero(message = "발행 수량은 0 이상이어야 합니다.")
    private Integer issueCount;

    @NotNull(message = "발행 시작 일수는 필수입니다.")
    private LocalDateTime issueStartAt;

    @Future(message = "발행 종료 일시는 미래여야 합니다.")
    private LocalDateTime issueEndAt;

    @PositiveOrZero(message = "유효 기간은 0일 이상이어야 합니다.")
    private Integer validPeriodDate;

    @Future(message = "유효 만료 일시는 미래여야 합니다.")
    private LocalDateTime validEndAt;
}
