package com.nhnacademy.member_server.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.feign.OrderFeignClient;
import com.nhnacademy.member_server.repository.GradeRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GradeSchedulerTest {

    @InjectMocks
    GradeScheduler gradeScheduler;

    @Mock
    MemberRepository memberRepository;

    @Mock
    GradeRepository gradeRepository;

    @Mock
    OrderFeignClient orderClient;

    @Test
    @DisplayName("등급 산정 테스트 - 등급 상향 (GENERAL -> GOLD)")
    void updateMemberGrades_UpgradeTest() {
        // [수정] self 필드 주입 (트랜잭션 프록시 시늉)
        org.springframework.test.util.ReflectionTestUtils.setField(gradeScheduler, "self", gradeScheduler);

        // given
        Grade general = Grade.builder().id(1L).gradeName("GENERAL").min(0).build();
        Grade gold = Grade.builder().id(2L).gradeName("GOLD").min(100000).build();
        List<Grade> grades = new ArrayList<>(List.of(general, gold));

        Member member = Member.builder()
                .id(1L)
                .grade(general)
                .status(Status.ACTIVE) // [수정] Status 설정 추가 (NPE 방지)
                .build();

        given(gradeRepository.findAll()).willReturn(grades);

        Pageable pageable0 = PageRequest.of(0, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable0)))
                .willReturn(new PageImpl<>(List.of(member)));

        Pageable pageable1 = PageRequest.of(1, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable1)))
                .willReturn(Page.empty());

        Map<Long, Long> orderStats = Map.of(1L, 150000L);
        given(orderClient.getBulkTotalAmounts(any(), any(LocalDateTime.class)))
                .willReturn(ResponseEntity.ok(orderStats));

        // when
        gradeScheduler.updateMemberGrades();

        // then
        assertThat(member.getGrade().getGradeName()).isEqualTo("GOLD");
        verify(memberRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("등급 산정 테스트 - 등급 유지 (주문 금액 부족)")
    void updateMemberGrades_NoChangeTest() {
        org.springframework.test.util.ReflectionTestUtils.setField(gradeScheduler, "self", gradeScheduler);

        // given
        Grade general = Grade.builder().id(1L).gradeName("GENERAL").min(0).build();
        Grade gold = Grade.builder().id(2L).gradeName("GOLD").min(100000).build();
        List<Grade> grades = new ArrayList<>(List.of(general, gold));

        Member member = Member.builder()
                .id(1L)
                .grade(general)
                .status(Status.ACTIVE) // [수정] Status 설정
                .build();

        given(gradeRepository.findAll()).willReturn(grades);

        Pageable pageable0 = PageRequest.of(0, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable0)))
                .willReturn(new PageImpl<>(List.of(member)));

        Pageable pageable1 = PageRequest.of(1, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable1)))
                .willReturn(Page.empty());

        Map<Long, Long> orderStats = Map.of(1L, 50000L);
        given(orderClient.getBulkTotalAmounts(any(), any(LocalDateTime.class)))
                .willReturn(ResponseEntity.ok(orderStats));

        // when
        gradeScheduler.updateMemberGrades();

        // then
        assertThat(member.getGrade().getGradeName()).isEqualTo("GENERAL");
        verify(memberRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("등급 산정 테스트 - 주문 서버 통신 실패 시 해당 페이지 스킵")
    void updateMemberGrades_OrderApiFailTest() {
        org.springframework.test.util.ReflectionTestUtils.setField(gradeScheduler, "self", gradeScheduler);

        // given
        Grade general = Grade.builder().id(1L).gradeName("GENERAL").min(0).build();
        List<Grade> grades = new ArrayList<>(List.of(general));

        Member member = Member.builder()
                .id(1L)
                .grade(general)
                .status(Status.ACTIVE) // [수정] Status 설정
                .build();

        given(gradeRepository.findAll()).willReturn(grades);

        Pageable pageable0 = PageRequest.of(0, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable0)))
                .willReturn(new PageImpl<>(List.of(member)));

        Pageable pageable1 = PageRequest.of(1, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable1)))
                .willReturn(Page.empty());

        given(orderClient.getBulkTotalAmounts(any(), any()))
                .willThrow(new RuntimeException("Connection Error"));

        // when
        gradeScheduler.updateMemberGrades();

        // then
        assertThat(member.getGrade().getGradeName()).isEqualTo("GENERAL");
        verify(memberRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("등급 산정 테스트 - 주문 데이터가 없을 경우(빈 Map) 0원으로 처리")
    void updateMemberGrades_EmptyStatsTest() {
        org.springframework.test.util.ReflectionTestUtils.setField(gradeScheduler, "self", gradeScheduler);

        // given
        Grade general = Grade.builder().id(1L).gradeName("GENERAL").min(0).build();
        List<Grade> grades = new ArrayList<>(List.of(general));

        Member member = Member.builder()
                .id(1L)
                .grade(general)
                .status(Status.ACTIVE) // [수정] Status 설정
                .build();

        given(gradeRepository.findAll()).willReturn(grades);

        Pageable pageable0 = PageRequest.of(0, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable0)))
                .willReturn(new PageImpl<>(List.of(member)));

        Pageable pageable1 = PageRequest.of(1, 1000);
        given(memberRepository.findAllByStatus((Status.ACTIVE), (pageable1)))
                .willReturn(Page.empty());

        given(orderClient.getBulkTotalAmounts(any(), any()))
                .willReturn(ResponseEntity.ok(Collections.emptyMap()));

        // when
        gradeScheduler.updateMemberGrades();

        // then
        assertThat(member.getGrade().getGradeName()).isEqualTo("GENERAL");
        verify(memberRepository, never()).saveAll(anyList());
    }
}