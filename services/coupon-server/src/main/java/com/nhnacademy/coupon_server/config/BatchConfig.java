package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.batch.BirthdayMemberItemReader;
import com.nhnacademy.coupon_server.batch.BirthdayMemberItemWriter;
import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import java.net.SocketTimeoutException;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final MemberServiceClient memberServiceClient;
    private final BirthdayMemberItemWriter birthdayMemberItemWriter;
    private static final int CHUNK_SIZE = 1000;
    @Value("${batch.retry.initial-interval:1000}") private long retryInitialInterval;
    @Value("${batch.retry.multiplier:2.0}") private double retryMultiplier;
    @Value("${batch.retry.max-interval:10000}") private long retryMaxInterval;
    @Value("${batch.retry.max-attempts:3}") private int retryMaxAttempts;

    // Reader 빈 등록
    @Bean
    @StepScope
    public BirthdayMemberItemReader birthdayMemberItemReader() {
        return new BirthdayMemberItemReader(memberServiceClient, CHUNK_SIZE);
    }

    // Job 등록 (배치 작업의 단위)
    @Bean
    public Job birthdayCouponJob(JobRepository jobRepository, Step birthdayCouponStep) {
        return new JobBuilder("birthdayCouponJob", jobRepository)
                .start(birthdayCouponStep) // 시작 Step 설정
                .build();
    }

    // Step 등록 (실제 처리 단계)
    @Bean
    public Step birthdayCouponStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   BirthdayMemberItemReader birthdayMemberItemReader) {

        // 재시도 대기 시간 정책 설정
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryInitialInterval); // 최초 대기 시간 (예: 1초)
        backOffPolicy.setMultiplier(retryMultiplier);        // 대기 시간 증가 배수 (예: 2배)
        backOffPolicy.setMaxInterval(retryMaxInterval);    // 최대 대기 시간 (예: 10초)

        return new StepBuilder("birthdayCouponStep", jobRepository)
                .<Long,Long>chunk(CHUNK_SIZE, transactionManager) // Chunk 기반 처리 설정 (입력 Long -> 출력 Long)
                .reader(birthdayMemberItemReader) // Reader 설정
                .writer(birthdayMemberItemWriter) // Writer 설정
                .faultTolerant() // 결함 허용 모드 활성화
                .retryLimit(retryMaxAttempts) // 최대 재시도 횟수 설정
                .retry(FeignException.class) // 재시도할 예외 1: 외부 API 호출 에러
                .retry(SocketTimeoutException.class) // 재시도할 예외 2: 타임아웃
                .backOffPolicy(backOffPolicy) // 위에서 설정한 백오프 정책 적용
                .build();
    }
}
