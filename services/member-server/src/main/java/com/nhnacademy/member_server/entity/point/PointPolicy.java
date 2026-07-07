package com.nhnacademy.member_server.entity.point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "point_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PointPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @LastModifiedDate // 엔티티 데이터 수정시 현재 일시 자동 저장
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer signupPoint;

    @Column(nullable = false)
    private Integer reviewPoint;

    @Column(nullable = false)
    private Integer photoPoint;

    @Builder
    private PointPolicy(int signupPoint, int reviewPoint, int photoPoint) {
        this.signupPoint = signupPoint;
        this.reviewPoint = reviewPoint;
        this.photoPoint = photoPoint;
    }
}
