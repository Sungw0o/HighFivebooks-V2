package com.nhnacademy.member_server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.member_server.repository.GradeRepository;
import com.nhnacademy.member_server.repository.PointPolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InitDataConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(InitDataConfig.class);

    @Test
    @DisplayName("프로필이 'test' -> 빈 생성 안 함 (CI/CD 테스트 환경 충돌 방지)")
    void whenProfileIsTest_thenBeanShouldNotBeCreated() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test")
                .withBean(PointPolicyRepository.class, () -> Mockito.mock(PointPolicyRepository.class))
                .withBean(GradeRepository.class, () -> Mockito.mock(GradeRepository.class))
                .run(context -> assertThat(context).doesNotHaveBean(CommandLineRunner.class));
    }
}