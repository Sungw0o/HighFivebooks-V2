package com.nhnacademy.payment_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SwaggerConfigTest {

    @Test
    void openAPICreated() {
        SwaggerConfig config = new SwaggerConfig();

        var openAPI = config.openAPI();

        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Payment Server API");
    }
}
