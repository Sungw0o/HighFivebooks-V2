package com.nhnacademy.order_server.listener;

import com.nhnacademy.order_server.config.RabbitMqConfig;
import com.nhnacademy.order_server.dto.message.PaymentSuccessMessage;
import com.nhnacademy.order_server.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentMessageListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_SUCCESS_QUEUE)
    public void handlePaymentSuccess(PaymentSuccessMessage message) {
        log.info("[RabbitMQ] payment success message received: orderId={}, paymentKey={}",
                message.getOrderId(), message.getPaymentKey());

        try {
            orderService.processPaymentSuccessMessage(message);

            log.info("[RabbitMQ] order post-processing completed: orderId={}", message.getOrderId());

        } catch (Exception e) {
            log.error("[RabbitMQ] order post-processing failed. Rabbit retry/DLQ policy will handle it. orderId={}",
                    message.getOrderId(), e);
            throw e;
        }
    }
}
