package com.nhnacademy.payment_server.config; // (멤버 서버는 패키지명 변경)

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Server API")
                        .description("결제 시스템 API 명세서입니다.")
                        .version("1.0.0"));
    }
}