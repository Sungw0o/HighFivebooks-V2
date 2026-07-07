package com.nhnacademy.payment_server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_method")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private String alias;

    @Builder
    public PaymentMethod(String name, String alias, boolean isActive) {
        this.name = name;
        this.alias = alias;
        this.isActive = isActive;
    }

    // 상태 변경 메서드 (관리자용)
    public void updateStatus(boolean isActive) {
        this.isActive = isActive;
    }
}
