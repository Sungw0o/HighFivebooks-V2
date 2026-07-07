package com.nhnacademy.member_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class RedisConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withPropertyValues(
                            "spring.profiles.active=local", // 👈 test가 아닌 프로파일
                            "spring.data.redis.host=localhost",
                            "spring.data.redis.port=6379",
                            "spring.data.redis.password=",
                            "spring.data.redis.database=0"
                    )
                    .withUserConfiguration(RedisConfig.class);

    @Test
    @DisplayName("RedisConfig 빈 등록 확인")
    void redisConfigLoads() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("redisTemplate");
            assertThat(context).hasBean("luaRedisTemplate");
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context).hasSingleBean(RedisConnectionFactory.class);
        });
    }

}
