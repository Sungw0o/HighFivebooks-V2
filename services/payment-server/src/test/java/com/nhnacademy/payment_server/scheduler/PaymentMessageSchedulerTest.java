package com.nhnacademy.payment_server.scheduler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.service.PaymentOutboxService;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentMessageSchedulerTest {

    @InjectMocks
    private PaymentMessageScheduler scheduler;

    @Mock
    private PaymentMessageOutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private PaymentOutboxService outboxService;

    @Test
    @DisplayName("대기 중인 메시지 없을 시 아무것도 안함")
    void sendPendingMessages_noMessages() {
        given(outboxRepository.findPendingMessages(eq(MessageStatus.READY), anyInt()))
                .willReturn(Collections.emptyList());

        scheduler.sendPendingMessages();

        verify(rabbitTemplate, never()).send(anyString(), any(Message.class));
    }

    @Test
    @DisplayName("메시지 전송 성공 시 DONE 상태 변경 및 서비스 호출")
    void sendPendingMessages_success() {
        PaymentMessageOutbox message = PaymentMessageOutbox.builder()
                .id(1L)
                .paymentId(100L)
                .payload("{\"orderId\":1}")
                .status(MessageStatus.READY)
                .createdAt(LocalDateTime.now())
                .build();

        given(outboxRepository.findPendingMessages(eq(MessageStatus.READY), anyInt()))
                .willReturn(List.of(message));

        scheduler.sendPendingMessages();

        verify(rabbitTemplate, times(1)).send(eq("payment-success-queue"), any(Message.class));

        assertThat(message.getStatus()).isEqualTo(MessageStatus.DONE);
    }

    @Test
    @DisplayName("RabbitMQ 전송 실패 시 재시도 카운트 증가 및 서비스 호출")
    void sendPendingMessages_failure() {
        PaymentMessageOutbox message = PaymentMessageOutbox.builder()
                .id(1L)
                .payload("{}")
                .status(MessageStatus.READY)
                .build();

        given(outboxRepository.findPendingMessages(eq(MessageStatus.READY), anyInt()))
                .willReturn(List.of(message));

        doThrow(new AmqpException("RabbitMQ Connection Failed"))
                .when(rabbitTemplate).send(anyString(), any(Message.class));

        scheduler.sendPendingMessages();

        verify(outboxService).increaseRetryCount(1L);
    }
}