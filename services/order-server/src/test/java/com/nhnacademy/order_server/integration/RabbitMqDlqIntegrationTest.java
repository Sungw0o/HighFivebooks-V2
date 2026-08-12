package com.nhnacademy.order_server.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.order_server.config.RabbitMqConfig;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_RABBITMQ_INTEGRATION", matches = "true")
class RabbitMqDlqIntegrationTest {

    private CachingConnectionFactory connectionFactory;
    private RabbitAdmin rabbitAdmin;

    @BeforeEach
    void setUp() {
        String host = System.getenv().getOrDefault("RABBITMQ_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("RABBITMQ_PORT", "5672"));
        connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(System.getenv().getOrDefault("RABBITMQ_USERNAME", "guest"));
        connectionFactory.setPassword(System.getenv().getOrDefault("RABBITMQ_PASSWORD", "guest"));
        rabbitAdmin = new RabbitAdmin(connectionFactory);

        RabbitMqConfig config = new RabbitMqConfig();
        rabbitAdmin.declareExchange(config.orderDeadLetterExchange());
        rabbitAdmin.declareQueue(config.orderPaymentDeadLetterQueue());
        rabbitAdmin.declareBinding(config.orderPaymentDeadLetterBinding());
        rabbitAdmin.declareQueue(config.paymentSuccessQueue());
        rabbitAdmin.purgeQueue(RabbitMqConfig.PAYMENT_SUCCESS_QUEUE, true);
        rabbitAdmin.purgeQueue(RabbitMqConfig.ORDER_PAYMENT_DEAD_LETTER_QUEUE, true);
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("처리 실패로 reject된 결제 성공 메시지는 실제 RabbitMQ DLQ로 이동한다")
    void rejectedPaymentMessageMovesToDlq() throws Exception {
        byte[] payload = "{\"orderId\":1,\"paymentKey\":\"failed\",\"totalAmount\":10000}"
                .getBytes(StandardCharsets.UTF_8);

        try (var connection = connectionFactory.getRabbitConnectionFactory().newConnection();
             var channel = connection.createChannel()) {
            channel.basicPublish("", RabbitMqConfig.PAYMENT_SUCCESS_QUEUE, null, payload);
            GetResponse sourceMessage = awaitMessage(channel, RabbitMqConfig.PAYMENT_SUCCESS_QUEUE);
            assertThat(sourceMessage).isNotNull();
            channel.basicReject(sourceMessage.getEnvelope().getDeliveryTag(), false);

            GetResponse deadLetter = awaitMessage(channel, RabbitMqConfig.ORDER_PAYMENT_DEAD_LETTER_QUEUE);
            assertThat(deadLetter).isNotNull();
            assertThat(deadLetter.getBody()).isEqualTo(payload);
            assertThat(deadLetter.getProps().getHeaders()).containsKey("x-death");
            channel.basicAck(deadLetter.getEnvelope().getDeliveryTag(), false);
        }
    }

    private GetResponse awaitMessage(com.rabbitmq.client.Channel channel, String queue) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        GetResponse response;
        do {
            response = channel.basicGet(queue, false);
            if (response != null) {
                return response;
            }
            Thread.sleep(50);
        } while (System.nanoTime() < deadline);
        return null;
    }
}
