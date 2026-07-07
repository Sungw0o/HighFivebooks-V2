package com.nhnacademy.payment_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
@Table(
        name = "payment_message_outbox",
        indexes = {
                @Index(name = "idx_outbox_status_created_at", columnList = "status, createdAt")
        }
)
public class PaymentMessageOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;      // 어떤 결제 건인지

    @Lob
    private String payload;      // 메시지 내용 (JSON String)

    @Setter
    @Enumerated(EnumType.STRING)
    private MessageStatus status; // READY(대기), DONE(완료)

    @CreatedDate
    private LocalDateTime createdAt;

    @Builder.Default // 빌더 패턴 쓸 때 기본값 0 적용
    private int retryCount = 0;

    public void incrementRetryCount() {
        this.retryCount++;
    }
}