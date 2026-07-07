package com.nhnacademy.coupon_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdaySchedulerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job birthdayCouponJob;

    @Mock
    private Job deleteExpiredCouponJob;

    private BirthdayScheduler birthdayScheduler;

    @BeforeEach
    void setUp() {
        birthdayScheduler = new BirthdayScheduler(
                jobLauncher,
                birthdayCouponJob,
                deleteExpiredCouponJob
        );
    }

    @Test
    @DisplayName("정상 동작: 스케줄러가 호출되면 배치 Job이 실행되어야 한다")
    void autoIssueBirthdayCoupons_Success() throws Exception {
        birthdayScheduler.autoIssueBirthdayCoupons();

        InOrder inOrder = inOrder(jobLauncher);
                inOrder.verify(jobLauncher).run(
                            eq(deleteExpiredCouponJob),
                            argThat(params -> "delete".equals(params.getString("type")))
                );
                inOrder.verify(jobLauncher).run(
                            eq(birthdayCouponJob),
                            argThat(params -> "birthday".equals(params.getString("type")))
                );
    }

    @Test
    @DisplayName("예외 처리: 삭제 Job 실패 시 생일 Job은 실행되지 않아야 한다 (현재 구조)")
    void autoIssueBirthdayCoupons_DeleteJobFails_BirthdayJobNotExecuted() throws Exception {
        when(jobLauncher.run(eq(deleteExpiredCouponJob), any(JobParameters.class)))
                .thenThrow(new JobExecutionAlreadyRunningException("Job is already running"));

        birthdayScheduler.autoIssueBirthdayCoupons();

        verify(jobLauncher, times(1)).run(eq(deleteExpiredCouponJob), any(JobParameters.class));
        verify(jobLauncher, never()).run(eq(birthdayCouponJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("예외 처리: 생일 Job 실패 시에도 스케줄러는 중단되지 않아야 한다")
    void autoIssueBirthdayCoupons_BirthdayJobFails() throws Exception {
        when(jobLauncher.run(eq(deleteExpiredCouponJob), any(JobParameters.class)))
                .thenReturn(mock(JobExecution.class));
        when(jobLauncher.run(eq(birthdayCouponJob), any(JobParameters.class)))
                .thenThrow(new JobRestartException("Cannot restart"));

        birthdayScheduler.autoIssueBirthdayCoupons();

        verify(jobLauncher, times(1)).run(eq(deleteExpiredCouponJob), any(JobParameters.class));
        verify(jobLauncher, times(1)).run(eq(birthdayCouponJob), any(JobParameters.class));
    }
}