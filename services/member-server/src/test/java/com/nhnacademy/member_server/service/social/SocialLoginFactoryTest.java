package com.nhnacademy.member_server.service.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class SocialLoginFactoryTest {

    @Test
    void getStrategy_Success() {
        SocialLoginStrategy mockStrategy = mock(SocialLoginStrategy.class);
        given(mockStrategy.getProviderName()).willReturn("PAYCO");

        SocialLoginFactory factory = new SocialLoginFactory(List.of(mockStrategy));

        SocialLoginStrategy result = factory.getStrategy("payco");

        assertThat(result).isEqualTo(mockStrategy);
    }

    @Test
    void getStrategy_Fail_NotSupported() {
        SocialLoginFactory factory = new SocialLoginFactory(Collections.emptyList());

        assertThatThrownBy(() -> factory.getStrategy("kakao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 소셜 로그인입니다");
    }
}