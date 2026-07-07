package com.nhnacademy.payment_server.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class TossPaymentConfig {
    @Value("${toss.secretKey}")
    private String secretKey;

    @Value("${toss.clientKey}")
    private String clientKey;
}