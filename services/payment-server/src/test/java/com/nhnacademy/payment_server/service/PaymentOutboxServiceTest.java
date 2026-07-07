package com.nhnacademy.payment_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxServiceTest {

    @InjectMocks
    private PaymentOutboxService outboxService;

    @Mock
    private PaymentMessageOutboxRepository outboxRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("재시도 횟수 증가 확인")
    void increaseRetryCount_success() {
        Long outboxId = 1L;
        PaymentMessageOutbox message = PaymentMessageOutbox.builder()
                .retryCount(0)
                .build();

        given(outboxRepository.findById(outboxId)).willReturn(Optional.of(message));

        outboxService.increaseRetryCount(outboxId);

        assertThat(message.getRetryCount()).isEqualTo(1);
    }


    @Test
    @DisplayName("Payment 저장 후 ID를 Outbox에 잘 전달하는지 확인")
    void savePaymentAndOutbox_success() {
        // 저장할 더미 데이터 준비
        Payment payment = Payment.builder().amount(1000L).build();
        PaymentMessageOutbox outbox = PaymentMessageOutbox.builder().payload("{}").build();

        // Payment 저장 시 ID가 100번인 객체가 반환된다고 가정 (Auto increment)
        Payment savedPayment = Payment.builder().amount(1000L).build();
        ReflectionTestUtils.setField(savedPayment, "id", 100L);

        given(paymentRepository.save(payment)).willReturn(savedPayment);

        Payment result = outboxService.savePaymentAndOutbox(payment, outbox);

        assertThat(result.getId()).isEqualTo(100L);

        // outboxRepository.save()에 넘겨진 객체를 Capture 후 검사
        ArgumentCaptor<PaymentMessageOutbox> captor = ArgumentCaptor.forClass(PaymentMessageOutbox.class);
        verify(outboxRepository).save(captor.capture());

        PaymentMessageOutbox finalOutbox = captor.getValue();

        // Payment의 ID가 Outbox의 paymentId에 잘 들어갔는지
        assertThat(finalOutbox.getPaymentId()).isEqualTo(100L);
        assertThat(finalOutbox.getStatus()).isEqualTo(MessageStatus.READY);
        assertThat(finalOutbox.getPayload()).isEqualTo("{}");
    }
}