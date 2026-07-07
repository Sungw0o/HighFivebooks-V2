package com.nhnacademy.coupon_server.entity;

import com.nhnacademy.coupon_server.entity.state.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_coupon",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "coupon_id"}))
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class MemberCoupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Setter
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Setter
    @Column(name = "issue_at", nullable = false)
    private LocalDateTime issueAt;

    @Setter
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id")
    private Long orderId;

    public void use(Long orderId) {
        validateUsable();
        this.status = Status.USED;
        this.orderId = orderId;
        this.usedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status != Status.USED) {
            throw new IllegalStateException("사용된 상태의 쿠폰만 취소할 수 있습니다.");
        }
        this.status = Status.ISSUED;
        this.usedAt = null;
        this.orderId = null;
    }

    public void validateOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new IllegalArgumentException("해당 쿠폰의 소유자가 아닙니다.");
        }
    }

    public void validateUsable() {
        if (this.status != Status.ISSUED) {
            throw new IllegalStateException("이미 사용했거나 사용할 수 없는 쿠폰입니다.");
        }
        if (this.expiredAt != null && this.expiredAt.isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("유효 기간이 지난 쿠폰입니다.");
        }
    }
}
