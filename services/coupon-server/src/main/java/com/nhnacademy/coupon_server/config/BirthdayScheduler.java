package com.nhnacademy.coupon_server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BirthdayScheduler {

    private final JobLauncher jobLauncher;
    private final Job birthdayCouponJob;
    private final Job deleteExpiredCouponJob;

    @Scheduled(cron = "0 0 0 1 * *")
    @SchedulerLock(name = "birthday_coupon_issue_lock", lockAtLeastFor = "PT30S", lockAtMostFor = "PT10M")
    public void autoIssueBirthdayCoupons() {
        log.info("매월 정기 쿠폰 작업(생일 쿠폰, 만료 정리) 시작");
        String now = LocalDateTime.now().toString();

        try {
            log.info(">>> 1. 만료/사용 쿠폰 정리 배치 시작");
            JobParameters deleteJobParams = new JobParametersBuilder()
                    .addString("executedAt", now + "_delete")
                    .addString("type", "delete")
                    .toJobParameters();
            jobLauncher.run(deleteExpiredCouponJob, deleteJobParams);
            log.info(">>> 1. 만료/사용 쿠폰 정리 배치 완료");

            log.info(">>> 2. 생일 쿠폰 발급 배치 시작");
            JobParameters birthdayJobParams = new JobParametersBuilder()
                    .addString("executedAt", now + "_birthday")
                    .addString("type", "birthday")
                    .toJobParameters();

            jobLauncher.run(birthdayCouponJob, birthdayJobParams);
            log.info(">>> 2. 생일 쿠폰 발급 배치 완료");

        } catch (Exception e) {
            log.error("생일 쿠폰 발급 배치 실행 중 오류 발생", e);
        }
    }
}