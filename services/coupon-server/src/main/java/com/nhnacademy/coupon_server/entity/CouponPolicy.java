package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "coupon_policy")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "coupon_policy_name", nullable = false)
    @Setter
    private String name;

    @Column(name = "comment", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter
    private Comment comment;

    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false)
    @Setter
    private Long discountValue;

    @Column(name = "min_order_value")
    @Setter
    private Long minOrderValue;

    @Column(name = "max_discount_value")
    @Setter
    private Long maxDiscountValue;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Setter
    private CouponPolicyStatus status = CouponPolicyStatus.ACTIVE;

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    @Builder.Default
    @BatchSize(size = 100)
    private Set<CouponPolicyBook> usableBooks = new HashSet<>();

    @OneToMany(mappedBy = "couponPolicy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter
    @Builder.Default
    private Set<CouponPolicyCategory> usableCategories = new HashSet<>();

    public void disable() {
        this.status = CouponPolicyStatus.INACTIVE;
    }

    public long calculateDiscountAmount(long orderPrice) {
        if (this.minOrderValue != null && orderPrice < this.minOrderValue) {
            throw new IllegalArgumentException("최소 주문 금액(" + this.minOrderValue + "원)을 충족하지 못했습니다.");
        }
        long discountAmount = 0;

        if (this.discountType == DiscountType.FIXED) {
            discountAmount = this.discountValue;
        } else if (this.discountType == DiscountType.PERCENTAGE) {
            discountAmount = java.math.BigDecimal.valueOf(orderPrice)
                    .multiply(java.math.BigDecimal.valueOf(this.discountValue))
                    .divide(java.math.BigDecimal.valueOf(100), RoundingMode.DOWN)
                    .longValue();
        }

        if (this.maxDiscountValue != null && discountAmount > this.maxDiscountValue) {
            discountAmount = this.maxDiscountValue;
        }

        return Math.min(discountAmount, orderPrice);
    }
}
