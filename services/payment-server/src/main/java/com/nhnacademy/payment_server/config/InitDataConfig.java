package com.nhnacademy.payment_server.config;

import com.nhnacademy.payment_server.service.PaymentMethodInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class InitDataConfig {

    private final PaymentMethodInitService initService;

    @Bean
    public CommandLineRunner initPaymentMethods() {
        return args -> initService.initializeMethods(); // 프록시 적용
    }
}