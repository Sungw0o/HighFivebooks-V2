package com.nhnacademy.payment_server.service;

import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.repository.PaymentMessageOutboxRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

    private final PaymentMessageOutboxRepository outboxRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseRetryCount(Long id) {
        outboxRepository.findById(id).ifPresent(PaymentMessageOutbox::incrementRetryCount);
    }

    @Transactional
    public Payment savePaymentAndOutbox(Payment payment, PaymentMessageOutbox outbox) {
        Payment savedPayment = paymentRepository.save(payment);

        PaymentMessageOutbox finalOutbox = PaymentMessageOutbox.builder()
                .paymentId(savedPayment.getId())
                .payload(outbox.getPayload())
                .status(MessageStatus.READY)
                .build();

        outboxRepository.save(finalOutbox);

        return savedPayment;
    }
}