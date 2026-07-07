package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentMethodInitService {

    private final PaymentMethodRepository repository;

    @Transactional
    public void initializeMethods() {
        List<PaymentMethod> targetMethods = List.of(
                PaymentMethod.builder().name("TOSS").alias("간편결제 / 카드결제").isActive(true).build(),
                PaymentMethod.builder().name("BANK_TRANSFER").alias("계좌이체").isActive(false).build(),
                PaymentMethod.builder().name("VIRTUAL_ACCOUNT").alias("무통장입금").isActive(false).build()
        );

        // N+1 문제 해결
        // DB에 있는 모든 이름을 한 번에 가져와서 Set
        Set<String> existingNames = repository.findAll().stream()
                .map(PaymentMethod::getName)
                .collect(Collectors.toSet());

        // 메모리 상에서 비교 후 없는 것만 필터링
        List<PaymentMethod> newMethods = targetMethods.stream()
                .filter(m -> !existingNames.contains(m.getName()))
                .toList();

        // 한 번에 저장
        if (!newMethods.isEmpty()) {
            repository.saveAll(newMethods);
        }
    }
}
