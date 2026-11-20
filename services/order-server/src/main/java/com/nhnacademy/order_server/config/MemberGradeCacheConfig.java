package com.nhnacademy.order_server.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@EnableCaching
public class MemberGradeCacheConfig {

    public static final String CACHE_NAME = "memberEarnRate";

    @Bean("memberGradeCacheManager")
    @ConditionalOnProperty(name = "order.member-grade-cache.type", havingValue = "caffeine", matchIfMissing = true)
    public CacheManager caffeineMemberGradeCacheManager(
            @Value("${order.member-grade-cache.ttl:5m}") String ttl,
            @Value("${order.member-grade-cache.maximum-size:10000}") long maximumSize) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_NAME);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(DurationStyle.detectAndParse(ttl))
                .recordStats());
        return cacheManager;
    }

    @Bean("memberGradeCacheManager")
    @ConditionalOnProperty(name = "order.member-grade-cache.type", havingValue = "redis")
    public CacheManager redisMemberGradeCacheManager(
            RedisConnectionFactory connectionFactory,
            @Value("${order.member-grade-cache.ttl:5m}") String ttl) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DurationStyle.detectAndParse(ttl))
                .disableCachingNullValues();
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .withInitialCacheConfigurations(java.util.Map.of(CACHE_NAME, configuration))
                .build();
    }

    @Bean("memberGradeCacheManager")
    @ConditionalOnProperty(name = "order.member-grade-cache.type", havingValue = "none")
    public CacheManager noOpMemberGradeCacheManager() {
        return new NoOpCacheManager();
    }
}
