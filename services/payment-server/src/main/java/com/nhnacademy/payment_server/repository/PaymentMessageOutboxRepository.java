package com.nhnacademy.payment_server.repository;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMessageOutboxRepository extends JpaRepository<PaymentMessageOutbox, Long> {
    @Query(value = """
        SELECT * FROM payment_message_outbox
        WHERE status = :#{#status.name()}
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<PaymentMessageOutbox> findPendingMessages(
            @Param("status") MessageStatus status,
            @Param("limit") int limit
    );
}
