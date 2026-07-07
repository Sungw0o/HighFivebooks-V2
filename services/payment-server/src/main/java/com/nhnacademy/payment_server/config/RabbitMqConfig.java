package com.nhnacademy.payment_server.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
