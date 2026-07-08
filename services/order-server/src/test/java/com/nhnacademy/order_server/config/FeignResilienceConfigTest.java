package com.nhnacademy.order_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Retryer;
import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class FeignResilienceConfigTest {

    @Test
    @DisplayName("Feign 기본 재시도를 비활성화한다")
    void feignRetryerDisablesImplicitRetry() {
        FeignResilienceConfig config = new FeignResilienceConfig();

        assertThat(config.feignRetryer()).isSameAs(Retryer.NEVER_RETRY);
    }

    @Test
    @DisplayName("local 프로필은 Feign timeout과 circuit breaker를 명시한다")
    void localProfileDefinesFeignTimeoutAndCircuitBreaker() throws IOException {
        PropertySource<?> source = loadYaml("application-local.yml");

        assertThat(source.getProperty("spring.cloud.openfeign.circuitbreaker.enabled")).isEqualTo(true);
        assertThat(source.getProperty("spring.cloud.openfeign.circuitbreaker.group.enabled")).isEqualTo(true);
        assertThat(source.getProperty("spring.cloud.openfeign.client.config.default.connectTimeout"))
                .isEqualTo("${FEIGN_CONNECT_TIMEOUT_MS:1000}");
        assertThat(source.getProperty("spring.cloud.openfeign.client.config.default.readTimeout"))
                .isEqualTo("${FEIGN_READ_TIMEOUT_MS:3000}");
        assertThat(source.getProperty("resilience4j.circuitbreaker.configs.default.failure-rate-threshold"))
                .isEqualTo("${CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD:50}");
    }

    @Test
    @DisplayName("prod 프로필은 Feign timeout과 circuit breaker를 명시한다")
    void prodProfileDefinesFeignTimeoutAndCircuitBreaker() throws IOException {
        PropertySource<?> source = loadYaml("application-prod.yml");

        assertThat(source.getProperty("spring.cloud.openfeign.circuitbreaker.enabled")).isEqualTo(true);
        assertThat(source.getProperty("spring.cloud.openfeign.circuitbreaker.group.enabled")).isEqualTo(true);
        assertThat(source.getProperty("spring.cloud.openfeign.client.config.default.connectTimeout"))
                .isEqualTo("${FEIGN_CONNECT_TIMEOUT_MS:1000}");
        assertThat(source.getProperty("spring.cloud.openfeign.client.config.default.readTimeout"))
                .isEqualTo("${FEIGN_READ_TIMEOUT_MS:3000}");
        assertThat(source.getProperty("resilience4j.circuitbreaker.configs.default.failure-rate-threshold"))
                .isEqualTo("${CIRCUIT_BREAKER_FAILURE_RATE_THRESHOLD:50}");
    }

    private PropertySource<?> loadYaml(String fileName) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        return loader.load(fileName, new ClassPathResource(fileName)).getFirst();
    }
}
