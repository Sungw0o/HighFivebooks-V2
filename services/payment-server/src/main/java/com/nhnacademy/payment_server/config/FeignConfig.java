package com.nhnacademy.payment_server.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Retryer retryer() {
        return Retryer.NEVER_RETRY; // feign 호출 실패시 기본 5회 재시도에서 즉시 실패하게 바꿈
    }
}