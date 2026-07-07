package com.nhnacademy.payment_server.repository;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    Optional<PaymentMethod> findByName(String name);
    boolean existsByName(String name);
}
