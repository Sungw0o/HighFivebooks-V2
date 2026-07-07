package com.nhnacademy.payment_server.repository;

import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 결제 취소용 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentKey = :paymentKey")
    Optional<Payment> findByPaymentKeyForUpdate(@Param("paymentKey") String paymentKey);

    // 상태별 총 금액 합계 (예: DONE 상태의 총합 = 총 매출)
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = :status")
    Optional<Long> sumAmountByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status IN :statuses")
    Optional<Long> sumAmountByStatusIn(@Param("statuses") List<PaymentStatus> statuses);

    // 상태별 건수
    long countByStatus(PaymentStatus status);

    // 특정 기간 일별 매출 통계 (JPQL + MySQL Date 함수)
    // approvedAt을 기준으로 날짜별(Date) 그룹핑하여 합계와 건수를 구함
    @Query("SELECT new com.nhnacademy.payment_server.dto.response.DailySalesResponse(" +
            "CAST(p.approvedAt AS LocalDate), SUM(p.amount), COUNT(p)) " +
            "FROM Payment p " +
            "WHERE p.status = 'DONE' AND p.approvedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY CAST(p.approvedAt AS LocalDate) " +
            "ORDER BY CAST(p.approvedAt AS LocalDate) ASC")
    List<DailySalesResponse> findDailySalesBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);
}
