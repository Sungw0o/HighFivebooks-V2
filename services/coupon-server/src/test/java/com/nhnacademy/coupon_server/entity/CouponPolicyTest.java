package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponPolicyTest {

    @Test
    @DisplayName("할인 계산 실패 - 최소 주문 금액 미충족")
    void calculateDiscountAmount_Fail_MinOrderValueNotMet() {
        CouponPolicy policy = CouponPolicy.builder()
                .minOrderValue(10000L)
                .build();

        long orderPrice = 5000L;

        assertThatThrownBy(() -> policy.calculateDiscountAmount(orderPrice))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소 주문 금액(10000원)을 충족하지 못했습니다.");
    }

    @Test
    @DisplayName("정액 할인 성공 - 일반적인 경우")
    void calculateDiscountAmount_Fixed_Success() {
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(5000L)
                .build();

        long orderPrice = 10000L;

        long discount = policy.calculateDiscountAmount(orderPrice);

        assertThat(discount).isEqualTo(1000L);
    }

    @Test
    @DisplayName("정액 할인 - 주문 금액보다 할인 금액이 클 경우 (주문 금액까지만 할인)")
    void calculateDiscountAmount_Fixed_CappedByOrderPrice() {
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(5000L) // 5000원 할인
                .minOrderValue(0L)
                .build();

        long orderPrice = 3000L; // 주문 금액이 할인액보다 적음

        long discount = policy.calculateDiscountAmount(orderPrice);

        assertThat(discount).isEqualTo(3000L); // 주문 금액인 3000원까지만 할인
    }

    @Test
    @DisplayName("정률 할인 성공 - 일반적인 경우 (10%)")
    void calculateDiscountAmount_Percentage_Success() {
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10L) // 10%
                .minOrderValue(5000L)
                .maxDiscountValue(10000L)
                .build();

        long orderPrice = 20000L;

        long discount = policy.calculateDiscountAmount(orderPrice);

        assertThat(discount).isEqualTo(2000L);
    }

    @Test
    @DisplayName("정률 할인 - 최대 할인 금액 제한 적용")
    void calculateDiscountAmount_Percentage_CappedByMaxDiscount() {
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(50L) // 50%
                .minOrderValue(1000L)
                .maxDiscountValue(5000L) // 최대 5000원까지만 할인
                .build();

        long orderPrice = 20000L; // 50% = 10000원이지만, 최대 한도는 5000원

        long discount = policy.calculateDiscountAmount(orderPrice);

        assertThat(discount).isEqualTo(5000L);
    }

    @Test
    @DisplayName("정률 할인 - 최대 할인 금액 설정이 없는 경우 (Null Safe)")
    void calculateDiscountAmount_Percentage_NoMaxLimit() {
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(20L) // 20%
                .minOrderValue(1000L)
                .maxDiscountValue(null) // 제한 없음
                .build();

        long orderPrice = 50000L;

        long discount = policy.calculateDiscountAmount(orderPrice);

        // Then: 50000 * 0.2 = 10000
        assertThat(discount).isEqualTo(10000L);
    }
    
    @Test
    @DisplayName("최소 주문 금액 설정이 없는 경우 (Null Safe)")
    void calculateDiscountAmount_NoMinOrderValue() {
        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .minOrderValue(null) // 최소 주문 금액 없음
                .build();

        long orderPrice = 500L; // 아주 작은 금액

        long discount = policy.calculateDiscountAmount(orderPrice);

        assertThat(discount).isEqualTo(500L); // 에러 없이 계산됨 (주문금액 상한 적용)
    }
}