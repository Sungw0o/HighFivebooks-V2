package com.nhnacademy.member_server.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.entity.point.PointEventType;
import com.nhnacademy.member_server.repository.GradeRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.impl.social.PaycoLoginStrategy;
import com.nhnacademy.member_server.service.social.SocialLoginFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class PointConcurrencyTest {

    @Autowired
    PointService pointService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    GradeRepository gradeRepository;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;
    @MockitoBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean
    private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean
    SocialLoginFactory socialLoginFactory;
    @MockitoBean
    PaycoLoginStrategy paycoLoginStrategy;
    @MockitoBean
    private JavaMailSender javaMailSender;
    @MockitoBean
    DefaultRedisScript<Long> redisScript;


    private Long memberId;

    @BeforeEach
    void setUp() {
        // 1. 등급 생성 (이미 존재하면 재사용하거나 초기화)
        gradeRepository.deleteAll(); // 테스트 격리를 위해 기존 데이터 삭제
        memberRepository.deleteAll();

        Grade grade = gradeRepository.save(Grade.builder()
                .gradeName("GENERAL").min(0).pointRate(new BigDecimal("0.01")).build());

        // 2. 회원 생성 (100,000원 보유)
        // 100원 * 1000번 요청을 모두 처리하려면 100,000원이 필요함
        Member member = memberRepository.save(Member.builder()
                .loginId("test").name("tester").password("1234").phone("010-0000-0000").email("test@test.com")
                .birthDate(LocalDate.now()).lastLoginAt(LocalDateTime.now())
                .status(Status.ACTIVE).role(Role.USER)
                .grade(grade)
                .currentPoint(100000L) // ★ 수정: 10,000 -> 100,000
                .build());

        this.memberId = member.getId();
    }

    @Test
    @DisplayName("동시성 테스트: 100원을 1000명이 동시에 사용 -> 잔액 0원 예상")
    void concurrentUsePoint() throws InterruptedException {
        int threadCount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(32); // 동시에 32개 스레드 수행
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            long orderId = i + 1000L;
            executor.submit(() -> {
                try {
                    // ★ 수정: usePoint -> createTransaction (Unified Method)
                    // ★ 수정: DTO -> PointTransactionCreateRequest Builder 패턴 사용
                    pointService.createTransaction(PointTransactionCreateRequest.builder()
                            .memberId(memberId)
                            .amount(100L)
                            .orderId(orderId)
                            .pointEventType(PointEventType.USE_ORDER) // 사용 타입 명시
                            .build());
                } catch (Exception e) {
                    // 동시성 이슈로 실패하더라도 로깅만 하고 진행 (테스트 목적상 락이 걸려있으면 실패 안 해야 정상)
                    e.printStackTrace();
                } finally {
                    latch.countDown(); // 작업 완료 카운트 감소
                }
            });
        }

        latch.await(); // 1000개 작업이 다 끝날 때까지 대기
        executor.shutdown();

        // 결과 검증
        Member member = memberRepository.findById(memberId).orElseThrow();

        // 100,000원에서 100원씩 1000번 뺐으니 0원이 되어야 함 (Lost Update가 없어야 함)
        assertThat(member.getCurrentPoint()).isZero();
    }
}