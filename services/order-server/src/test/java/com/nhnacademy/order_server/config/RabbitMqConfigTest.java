package com.nhnacademy.order_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    @DisplayName("payment-success-queue는 주문 결제 DLQ로 dead-letter 라우팅된다")
    void paymentSuccessQueueHasDeadLetterRouting() {
        Queue queue = config.paymentSuccessQueue();

        assertThat(queue.getName()).isEqualTo(RabbitMqConfig.PAYMENT_SUCCESS_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMqConfig.ORDER_DEAD_LETTER_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitMqConfig.ORDER_PAYMENT_DEAD_LETTER_ROUTING_KEY);
    }

    @Test
    @DisplayName("주문 결제 DLQ exchange/queue/binding을 선언한다")
    void orderPaymentDeadLetterTopology() {
        TopicExchange exchange = config.orderDeadLetterExchange();
        Queue dlq = config.orderPaymentDeadLetterQueue();
        Binding binding = config.orderPaymentDeadLetterBinding();

        assertThat(exchange.getName()).isEqualTo(RabbitMqConfig.ORDER_DEAD_LETTER_EXCHANGE);
        assertThat(dlq.getName()).isEqualTo(RabbitMqConfig.ORDER_PAYMENT_DEAD_LETTER_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(RabbitMqConfig.ORDER_DEAD_LETTER_EXCHANGE);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitMqConfig.ORDER_PAYMENT_DEAD_LETTER_ROUTING_KEY);
        assertThat(binding.getDestination()).isEqualTo(RabbitMqConfig.ORDER_PAYMENT_DEAD_LETTER_QUEUE);
    }
}
