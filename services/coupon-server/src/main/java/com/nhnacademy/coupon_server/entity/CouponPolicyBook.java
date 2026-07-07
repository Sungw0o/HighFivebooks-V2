package com.nhnacademy.coupon_server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupon_policy_book")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponPolicyBook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private CouponPolicy couponPolicy;

    @Column(name = "book_id")
    private Long bookId;
}
