package com.nhnacademy.payment_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

    @Test
    void tossRestClientCreated() {
        // given
        RestClientConfig config = new RestClientConfig();

        // when
        RestClient client = config.tossRestClient();

        // then
        assertThat(client).isNotNull();
    }
}
