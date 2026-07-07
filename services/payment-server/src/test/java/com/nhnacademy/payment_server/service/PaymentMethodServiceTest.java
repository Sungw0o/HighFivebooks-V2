package com.nhnacademy.payment_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.nhnacademy.payment_server.dto.response.PaymentMethodResponse;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.service.impl.PaymentMethodServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @InjectMocks
    PaymentMethodServiceImpl paymentMethodService;

    @Test
    @DisplayName("모든 결제수단 조회 성공")
    void getAllMethods_success() {
        PaymentMethod m1 = PaymentMethod.builder().name("TOSS").isActive(true).build();
        PaymentMethod m2 = PaymentMethod.builder().name("CARD").isActive(false).build();

        ReflectionTestUtils.setField(m1, "id", 1L);
        ReflectionTestUtils.setField(m2, "id", 2L);

        when(paymentMethodRepository.findAll()).thenReturn(List.of(m1, m2));

        List<PaymentMethodResponse> list = paymentMethodService.getAllMethods();

        assertThat(list).hasSize(2);
        assertThat(list.get(1).getName()).isEqualTo("CARD");
    }

    @Test
    @DisplayName("결제수단 상태 변경 성공")
    void updateStatus_success() {
        PaymentMethod m1 = PaymentMethod.builder().name("TOSS").isActive(true).build();

        ReflectionTestUtils.setField(m1, "id", 1L);

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(m1));

        paymentMethodService.updateStatus(1L, false);

        assertThat(m1.isActive()).isFalse();
    }

    @Test
    @DisplayName("결제수단 상태 변경 실패 - 존재하지 않는 ID")
    void updateStatus_notFound() {
        when(paymentMethodRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> paymentMethodService.updateStatus(99L, true)
        );

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.METHOD_NOT_FOUND);
    }

}
