package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "coupon_name", nullable = false)
    private String couponName;

    @Column(name = "description")
    private String description;

    @Column(name = "coupon_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CouponType couponType = CouponType.NORMAL;

    @Column(name = "issue_count")
    private Integer issueCount;

    /*
     * ERD에서는 Timestamp를 썼지만 LocalDateTime을 쓰는 이유는 객체가 선언될 때 값이 변경될 수 있기 때문이다.
     * LocalDateTime은 이 불변성을 해결할 수 있음. 날짜를 변경할 경우 기존 객체를 변경하지 않고, 새로이 객체를 반환하기 때문에 데이터가 꼬일 일이 없음.
     * 이렇게 달라보여도 JPA에서는 자동으로 변환해준다. 이렇게 다른 이유는 Timestamp는 DB에 저장 효율을 높이고, 코드에서는 개발 편의성과 안정성을 위해서 LocalDateTime을 사용한다.
     */
    @Column(name = "issued_start_at")
    private LocalDateTime issuedStartAt;

    @Column(name = "issue_end_at")
    private LocalDateTime issuedEndAt;

    @Column(name = "valid_period_date")
    private Integer validPeriodDate;

    @Column(name = "valid_end_at")
    private LocalDateTime validEndAt;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_policy_id", nullable = false)
    private CouponPolicy couponPolicy;

    public void updateStatus(CouponStatus newStatus) {
        this.status = newStatus;
    }
}
