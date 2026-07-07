package com.nhnacademy.coupon_server.dto.request;

import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CouponPolicyRequestDto {
    @NotBlank(message = "쿠폰 정책 이름은 필수입니다.")
    private String name;

    @NotNull(message = "쿠폰 용도는 필수입니다.")
    private Comment comment;

    @NotNull(message = "할인 유형은 필수입니다.")
    private DiscountType discountType;

    @NotNull(message = "할인 값은 필수입니다.")
    @PositiveOrZero(message = "할인 값은 0 이상이어야 합니다.")
    private Long discountValue;

    @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
    private Long minOrderValue;

    @AssertTrue(message = "정률(%) 할인은 100을 초과할 수 없습니다.")
    public boolean isValidPercentage() {
        if (this.discountType == DiscountType.PERCENTAGE) {
            return this.discountValue != null && this.discountValue <= 100;
        }
        return true;
    }

    @PositiveOrZero(message = "최대 할인 금액은 0 이상이어야 합니다.")
    private Long maxDiscountValue;

    @PositiveOrZero(message = "유효 기간(일)은 0 이상이어야 합니다.")
    private Integer validPeriodDate;

    private List<Long> targetBookIds;

    private List<Long> targetCategoryIds;

    @AssertTrue(message = "정액 할인의 경우 할인 금액이 최소 주문 금액보다 클 수 없습니다.")
    public boolean isDiscountLessThanMinOrder() {
        if (this.discountType == DiscountType.FIXED && this.minOrderValue != null) {
            // 최소 주문 금액이 0원이면 검사 생략 (조건 없음)
            if (this.minOrderValue == 0) return true;
            return this.discountValue <= this.minOrderValue;
        }
        return true;
    }
}
