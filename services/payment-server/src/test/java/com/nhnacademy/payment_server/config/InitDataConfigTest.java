package com.nhnacademy.payment_server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.service.PaymentMethodInitService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InitDataConfigTest {

    @Mock
    private PaymentMethodRepository repository;

    @InjectMocks
    private PaymentMethodInitService initService;

    @Test
    @DisplayName("DB에 데이터가 없으면 3개 모두 저장 (Branch: True)")
    void initializeMethods_saveAll() {
        given(repository.findAll()).willReturn(Collections.emptyList());

        initService.initializeMethods(); // 서비스 메서드 직접 호출

        ArgumentCaptor<List<PaymentMethod>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());

        List<PaymentMethod> savedMethods = captor.getValue();
        assertThat(savedMethods).hasSize(3);
        assertThat(savedMethods)
                .extracting(PaymentMethod::getName)
                .containsExactlyInAnyOrder("TOSS", "BANK_TRANSFER", "VIRTUAL_ACCOUNT");
    }

    @Test
    @DisplayName("DB에 이미 데이터가 있으면 저장x (Branch: False)")
    void initializeMethods_skipSave() {
        List<PaymentMethod> existingMethods = List.of(
                PaymentMethod.builder().name("TOSS").alias("간편결제 / 카드결제").isActive(true).build(),
                PaymentMethod.builder().name("BANK_TRANSFER").alias("계좌이체").isActive(false).build(),
                PaymentMethod.builder().name("VIRTUAL_ACCOUNT").alias("무통장입금").isActive(false).build()
        );
        given(repository.findAll()).willReturn(existingMethods);

        initService.initializeMethods();

        verify(repository, never()).saveAll(anyList());
    }
}