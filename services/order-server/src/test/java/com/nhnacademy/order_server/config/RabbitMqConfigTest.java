package com.nhnacademy.order_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

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

    @Test
    @DisplayName("local 프로필은 Rabbit listener retry backoff를 명시한다")
    void localProfileDefinesRabbitRetryBackoff() throws IOException {
        assertRabbitRetryProperties(loadYaml("application-local.yml"));
    }

    @Test
    @DisplayName("prod 프로필은 Rabbit listener retry backoff를 명시한다")
    void prodProfileDefinesRabbitRetryBackoff() throws IOException {
        assertRabbitRetryProperties(loadYaml("application-prod.yml"));
    }

    private void assertRabbitRetryProperties(PropertySource<?> source) {
        assertThat(source.getProperty("spring.rabbitmq.listener.simple.retry.max-attempts"))
                .isEqualTo("${RABBIT_LISTENER_RETRY_MAX_ATTEMPTS:3}");
        assertThat(source.getProperty("spring.rabbitmq.listener.simple.retry.initial-interval"))
                .isEqualTo("${RABBIT_LISTENER_RETRY_INITIAL_INTERVAL_MS:1000}");
        assertThat(source.getProperty("spring.rabbitmq.listener.simple.retry.multiplier"))
                .isEqualTo("${RABBIT_LISTENER_RETRY_MULTIPLIER:2.0}");
        assertThat(source.getProperty("spring.rabbitmq.listener.simple.retry.max-interval"))
                .isEqualTo("${RABBIT_LISTENER_RETRY_MAX_INTERVAL_MS:10000}");
    }

    private PropertySource<?> loadYaml(String fileName) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        return loader.load(fileName, new ClassPathResource(fileName)).getFirst();
    }
}
