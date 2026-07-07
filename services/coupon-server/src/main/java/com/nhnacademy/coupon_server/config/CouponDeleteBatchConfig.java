package com.nhnacademy.coupon_server.config;

import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CouponDeleteBatchConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final MemberCouponRepository memberCouponRepository;
    private static final int CHUNK_SIZE = 1000;

    // Job 설정
    @Bean
    public Job deleteExpiredCouponJob(JobRepository jobRepository, Step deleteExpiredCouponStep) {
        return new JobBuilder("deleteExpiredCouponJob", jobRepository)
                .start(deleteExpiredCouponStep)
                .build();
    }

    // Step 설정
    @Bean
    public Step deleteExpiredCouponStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager) {
        return new StepBuilder("deleteExpiredCouponStep", jobRepository)
                .<MemberCoupon, MemberCoupon>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredOrUsedCouponReader()) // Reader
                .writer(couponDeleteWriter()) // Writer
                .build();
    }

    // Reader: 만료되었거나(ISSUED & 시간경과) 사용된(USED) 쿠폰 조회
    @Bean
    @StepScope
    public JpaPagingItemReader<MemberCoupon> expiredOrUsedCouponReader() {
        return new JpaPagingItemReaderBuilder<MemberCoupon>()
                .name("expiredOrUsedCouponReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                // JPQL을 사용하여 조건에 맞는 쿠폰 조회 (Fetch Join으로 연관 엔티티도 한 번에 로딩)
                .queryString("SELECT mc FROM MemberCoupon mc " +
                        "JOIN FETCH mc.coupon c " +
                        "JOIN FETCH c.couponPolicy cp " +
                        "WHERE mc.status = :usedStatus OR (mc.status = :issuedStatus AND mc.expiredAt < :now)")
                .parameterValues(Map.of(
                        "usedStatus", Status.USED,
                        "issuedStatus", Status.ISSUED,
                        "now", LocalDateTime.now()
                ))
                .build();
    }

    // Writer: 조회된 쿠폰들을 삭제 및 재고 복구 처리
    @Bean
    public ItemWriter<MemberCoupon> couponDeleteWriter() {
        return items -> {
            log.info("삭제 대상 쿠폰 {}건 삭제 진행", items.size());
            // DB에서 쿠폰 데이터 삭제
            memberCouponRepository.deleteAll(items);
        };
    }
}