package com.nhnacademy.member_server.config;

import com.nhnacademy.member_server.entity.point.PointPolicy;
import com.nhnacademy.member_server.repository.PointPolicyRepository;
import com.nhnacademy.member_server.service.GradeInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Slf4j
public class InitDataConfig {

    @Bean
    public CommandLineRunner initData(PointPolicyRepository policyRepository,
                                      GradeInitService gradeInitService) {
        return args -> {
            // 1. 포인트 정책 초기화
            if (policyRepository.count() == 0) {
                PointPolicy defaultPolicy = PointPolicy.builder()
                        .signupPoint(5000)
                        .reviewPoint(200)
                        .photoPoint(500)
                        .build();
                policyRepository.save(defaultPolicy);
            }

            // 2. 등급 정책 초기화 (각 등급별로 존재하는지 체크 후 생성)
            gradeInitService.createGradeIfNotExists("GENERAL", 0, 100000, "0.01");
            gradeInitService.createGradeIfNotExists("ROYAL", 100000, 200000, "0.02");
            gradeInitService.createGradeIfNotExists("GOLD", 200000, 300000, "0.025");
            gradeInitService.createGradeIfNotExists("PLATINUM", 300000, null, "0.03");

            log.info(">>> [INIT] 등급 데이터 점검 및 초기화 완료");
        };
    }
}