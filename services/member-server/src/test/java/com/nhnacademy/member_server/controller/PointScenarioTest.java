package com.nhnacademy.member_server.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PointScenarioTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired GradeRepository gradeRepository;

    @MockitoBean(name = "cartTTLScanScheduler")
    private Object cartTTLScanScheduler;

    @MockitoBean private RedisTemplate<String, Object> redisTemplate;
    @MockitoBean private RedisConnectionFactory redisConnectionFactory;
    @MockitoBean private ReactiveRedisConnectionFactory reactiveRedisConnectionFactory;
    @MockitoBean private SocialLoginFactory socialLoginFactory;
    @MockitoBean private PaycoLoginStrategy paycoLoginStrategy;
    @MockitoBean private DefaultRedisScript<Long> redisScript;
    @MockitoBean private JavaMailSender javaMailSender;

    private Long memberId;

    private static final String BASE_URL = "/internal/point-transactions";

    @BeforeEach
    void setUp() {
        Grade grade = gradeRepository.save(Grade.builder()
                .gradeName("GENERAL").min(0).pointRate(new BigDecimal("0.01")).build());

        Member member = memberRepository.save(Member.builder()
                .loginId("scenario_tester")
                .name("김유저")
                .password("1234")
                .phone("010-1111-2222")
                .email("test@scenario.com")
                .birthDate(LocalDate.now())
                .lastLoginAt(LocalDateTime.now())
                .status(Status.ACTIVE)
                .role(Role.USER)
                .grade(grade)
                .currentPoint(0L)
                .build());

        this.memberId = member.getId();
    }

    @Test
    @DisplayName("시나리오: 적립(구매) -> 사용(구매) -> 사용 취소(단순취소) -> 잔액 확인")
    void pointLifecycleScenario() throws Exception {

        // 1. 상품 구매 적립 (EARN_ORDER)
        // 예상: 500,000 * 0.01 = 5,000 포인트 적립
        PointTransactionCreateRequest earnRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_ORDER)
                .amount(500000L)
                .orderId(1001L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                // PointTransactionResponse 객체 내의 currentPoint 필드 확인
                .andExpect(jsonPath("$.currentPoint").value(5000));


        // 2. 포인트 사용 (USE_ORDER)
        // 예상: 5,000 - 2,000 = 3,000 포인트 잔액
        PointTransactionCreateRequest useRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_ORDER)
                .amount(2000L)
                .orderId(1002L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(3000));


        // 3. 결제 취소로 인한 사용 포인트 복구 (USE_CANCEL_ORDER)
        // 예상: 3,000 + 2,000 = 5,000 포인트 잔액
        PointTransactionCreateRequest revertRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_CANCEL_ORDER)
                .amount(2000L)
                .orderId(1002L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revertRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(5000));
    }

    @Test
    @DisplayName("시나리오: 적립 -> 사용 -> 반품 (사용 포인트 복구 + 적립 포인트 회수)")
    void returnRevertScenario() throws Exception {

        // 1. 초기 적립 (EARN_ORDER)
        // 1,000,000 * 0.01 = 10,000 포인트
        PointTransactionCreateRequest earnRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_ORDER)
                .amount(1000000L)
                .orderId(2001L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(10000));


        // 2. 포인트 사용 (USE_ORDER)
        // 10,000 - 3,000 = 7,000 포인트
        PointTransactionCreateRequest useRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_ORDER)
                .amount(3000L)
                .orderId(2001L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(7000));


        // 3-1. [반품] 사용했던 포인트 복구 (USE_CANCEL_RETURN)
        // 7,000 + 3,000 = 10,000 포인트
        PointTransactionCreateRequest useCancelRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_CANCEL_RETURN)
                .amount(3000L)
                .orderId(2001L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useCancelRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(10000));


        // 3-2. [반품] 적립되었던 포인트 회수 (EARN_CANCEL_RETURN)
        // 10,000 - 10,000 = 0 포인트
        // (금액을 0으로 보내도 Service 로직에서 원본 주문(2001)의 적립금을 찾아 자동 차감)
        PointTransactionCreateRequest earnCancelRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_CANCEL_RETURN)
                .amount(0L)
                .orderId(2001L)
                .build();

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnCancelRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoint").value(0));
    }
}