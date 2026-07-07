package com.nhnacademy.payment_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_status_approved_at", columnList = "status, approvedAt")
        }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_method_id", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(nullable = false)
    private Long orderId; // 주문 ID FK

    @CreatedDate // 결제 요청 시간 자동 저장
    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @Setter
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private PaymentStatus status;

    @Column(nullable = false, length = 200, unique = true)
    private String paymentKey;

    @Column(nullable = false)
    private Long amount;

    @Builder
    public Payment(PaymentMethod paymentMethod, Long orderId, LocalDateTime requestedAt, LocalDateTime approvedAt, PaymentStatus status,
                   String paymentKey, Long amount) {
        this.paymentMethod = paymentMethod;
        this.orderId = orderId;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.status = status;
        this.paymentKey = paymentKey;
        this.amount = amount;
    }
}
