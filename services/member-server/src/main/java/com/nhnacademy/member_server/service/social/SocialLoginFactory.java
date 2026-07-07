package com.nhnacademy.member_server.service.social;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;


@Component
public class SocialLoginFactory {

    private final Map<String, SocialLoginStrategy> strategies;

    public SocialLoginFactory(List<SocialLoginStrategy> strategyList) {
        Map<String, SocialLoginStrategy> map = new HashMap<>();

        for (SocialLoginStrategy strategy : strategyList) {
            String key = strategy.getProviderName().toUpperCase();

            map.put(key, strategy);
        }

        this.strategies = map;
    }

    public SocialLoginStrategy getStrategy(String provider) {
        SocialLoginStrategy strategy = strategies.get(provider.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인입니다: " + provider);
        }
        return strategy;
    }
}