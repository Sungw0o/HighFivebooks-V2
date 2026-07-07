package com.nhnacademy.member_server.repository;

import com.nhnacademy.member_server.entity.point.PointEventType;
import com.nhnacademy.member_server.entity.point.PointHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Page<PointHistory> findAllByMemberId(Long memberId, Pageable pageable);

    // 반품으로 인한 포인트 환불금 제외 적립된 금액만 다 더하는 쿼리
    @Query("SELECT COALESCE(SUM(ph.amount), 0) FROM PointHistory ph " +
            "WHERE ph.member.id = :memberId " +
            "AND ph.amount > 0 " +
            "AND ph.pointEventType != 'EARN_REFUND'")
    Long sumEarnedPoints(@Param("memberId") Long memberId);

    // 멱등성 검사용
    boolean existsByOrderIdAndPointEventType(Long orderId, PointEventType eventType);

    // 상태 변경을 위한 단건 조회용
    Optional<PointHistory> findByOrderIdAndPointEventType(Long orderId, PointEventType pointEventType);
}
