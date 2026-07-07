package com.nhnacademy.coupon_server.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    public static final String COUPON_WELCOME_QUEUE = "high-five-coupon-welcome-queue";
    public static final String COUPON_ISSUE_QUEUE = "high-five-coupon-issue-queue";
    public static final String COUPON_DEAD_LETTER_EXCHANGE = "high-five-coupon-dead-letter-exchange";
    public static final String COUPON_DEAD_LETTER_QUEUE = "high-five-coupon-dead-letter-queue";
    public static final String COUPON_DEAD_LETTER_ROUTING_KEY = "high-five.dead.letter";

    @Bean
    public Queue welcomeCouponQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", COUPON_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", COUPON_DEAD_LETTER_ROUTING_KEY);
        return new Queue(COUPON_WELCOME_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue issuesCouponQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", COUPON_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", COUPON_DEAD_LETTER_ROUTING_KEY);
        return new Queue(COUPON_ISSUE_QUEUE, true, false, false, args);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(COUPON_DEAD_LETTER_EXCHANGE);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(COUPON_DEAD_LETTER_QUEUE, true);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(COUPON_DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(5);
        factory.setPrefetchCount(10);
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        // 재시도 인터셉터 설정
        RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
                .maxAttempts(3) // 최대 3번 시도 (초기 1회 + 재시도 2회)
                .backOffOptions(1000, 2.0, 10000) // 1초 대기, 2배씩 증가, 최대 10초
                .recoverer(new RejectAndDontRequeueRecoverer()) // 재시도 실패 시 예외 던짐 -> DLQ 설정에 의해 DLQ로 이동
                .build();

        factory.setAdviceChain(retryInterceptor);
        return factory;
    }

}