package com.nhnacademy.order_server.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {

    public static final String PAYMENT_SUCCESS_QUEUE = "payment-success-queue";
    public static final String ORDER_DEAD_LETTER_EXCHANGE = "high-five-order-dead-letter-exchange";
    public static final String ORDER_PAYMENT_DEAD_LETTER_QUEUE = "high-five-order-payment-dead-letter-queue";
    public static final String ORDER_PAYMENT_DEAD_LETTER_ROUTING_KEY = "high-five.order.payment.dead.letter";

    @Bean
    public Queue paymentSuccessQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", ORDER_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", ORDER_PAYMENT_DEAD_LETTER_ROUTING_KEY);
        return new Queue(PAYMENT_SUCCESS_QUEUE, true, false, false, args);
    }

    @Bean
    public TopicExchange orderDeadLetterExchange() {
        return new TopicExchange(ORDER_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue orderPaymentDeadLetterQueue() {
        return new Queue(ORDER_PAYMENT_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding orderPaymentDeadLetterBinding() {
        return BindingBuilder.bind(orderPaymentDeadLetterQueue())
                .to(orderDeadLetterExchange())
                .with(ORDER_PAYMENT_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter,
            @Value("${spring.rabbitmq.listener.simple.auto-startup:true}") boolean autoStartup,
            @Value("${spring.rabbitmq.listener.simple.retry.max-attempts:3}") int maxAttempts,
            @Value("${spring.rabbitmq.listener.simple.retry.initial-interval:1000}") long initialInterval,
            @Value("${spring.rabbitmq.listener.simple.retry.multiplier:2.0}") double multiplier,
            @Value("${spring.rabbitmq.listener.simple.retry.max-interval:10000}") long maxInterval
    ) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAutoStartup(autoStartup);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);

        RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialInterval, multiplier, maxInterval)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();

        factory.setAdviceChain(retryInterceptor);
        return factory;
    }
}
