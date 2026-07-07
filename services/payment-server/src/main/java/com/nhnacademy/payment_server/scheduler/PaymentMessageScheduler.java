package com.nhnacademy.payment_server.scheduler;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.service.PaymentOutboxService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageScheduler {

    private final PaymentMessageOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentOutboxService outboxService;
    private static final int MAX_RETRY_COUNT = 3;

    // 2초마다 실행
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void sendPendingMessages() {
        List<PaymentMessageOutbox> messages =
                outboxRepository.findPendingMessages(MessageStatus.READY, 10);

        if (messages.isEmpty()) {
            return;
        }

        for (PaymentMessageOutbox outboxMsg : messages) {
            if (outboxMsg.getRetryCount() >= MAX_RETRY_COUNT) {
                log.warn("메시지 전송 실패 횟수 초과. 상태: FAILED. id={}", outboxMsg.getId());
                outboxMsg.setStatus(MessageStatus.FAILED);
                continue;
            }

            try {
                // 이미 JSON 문자열인 payload를 바이트 배열로 변환하여 전송
                // Content-Type을 application/json으로 명시해야 받는 쪽에서 JSON으로 인식함
                Message message = MessageBuilder
                        .withBody(outboxMsg.getPayload().getBytes(StandardCharsets.UTF_8))
                        .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                        .build();

                rabbitTemplate.send("payment-success-queue", message);

                outboxMsg.setStatus(MessageStatus.DONE);
                log.info("지연 메시지 발송 성공: outboxId={}", outboxMsg.getId());

            } catch (Exception e) {
                log.error("메시지 발송 실패. count={} id={}", outboxMsg.getRetryCount() + 1, outboxMsg.getId(), e);
                outboxService.increaseRetryCount(outboxMsg.getId());
            }
        }
    }
}