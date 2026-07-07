package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.batch.BirthdayMemberItemReader;
import com.nhnacademy.coupon_server.batch.BirthdayMemberItemWriter;
import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BatchConfigTest {

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private BirthdayMemberItemWriter birthdayMemberItemWriter;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private BatchConfig batchConfig;

    @BeforeEach
    void setUp() {
        batchConfig = new BatchConfig(memberServiceClient, birthdayMemberItemWriter);

        ReflectionTestUtils.setField(batchConfig, "retryInitialInterval", 1000L);
        ReflectionTestUtils.setField(batchConfig, "retryMultiplier", 2.0);
        ReflectionTestUtils.setField(batchConfig, "retryMaxInterval", 10000L);
        ReflectionTestUtils.setField(batchConfig, "retryMaxAttempts", 3);
    }

    @Test
    @DisplayName("BirthdayMemberItemReader 빈 생성 테스트 (@StepScope 메서드 검증)")
    void birthdayMemberItemReader_CreatesInstance() {
        BirthdayMemberItemReader reader = batchConfig.birthdayMemberItemReader();

        assertThat(reader).isNotNull();
    }

    @Test
    @DisplayName("BirthdayCouponJob 빈 생성 테스트")
    void birthdayCouponJob_CreatesJob() {
        Step step = mock(Step.class);

        Job job = batchConfig.birthdayCouponJob(jobRepository, step);

        assertThat(job).isNotNull();
        assertThat(job.getName()).isEqualTo("birthdayCouponJob");
    }

    @Test
    @DisplayName("BirthdayCouponStep 빈 생성 테스트 - Retry 정책 설정 포함")
    void birthdayCouponStep_CreatesStep() {
        BirthdayMemberItemReader reader = mock(BirthdayMemberItemReader.class);

        Step step = batchConfig.birthdayCouponStep(jobRepository, transactionManager, reader);

        assertThat(step).isNotNull();
        assertThat(step.getName()).isEqualTo("birthdayCouponStep");
    }
}