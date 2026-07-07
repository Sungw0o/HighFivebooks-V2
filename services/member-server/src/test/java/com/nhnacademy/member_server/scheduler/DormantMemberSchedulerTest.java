package com.nhnacademy.member_server.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.repository.MemberRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DormantMemberSchedulerTest {

    @InjectMocks
    private DormantMemberScheduler scheduler;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("휴면 회원 전환 배치 테스트 - Bulk Update 호출 검증")
    void processDormantMembersTest() {
        // given
        // 리포지토리가 10명을 업데이트했다고 가정 (반환값 설정)
        int expectedUpdatedCount = 10;

        given(memberRepository.bulkUpdateDormantMembers(
                any(LocalDateTime.class),
                eq(Status.ACTIVE),
                eq(Status.DORMANT)
        )).willReturn(expectedUpdatedCount);

        // when
        scheduler.processDormantMembers();


        verify(memberRepository, times(1)).bulkUpdateDormantMembers(
                any(LocalDateTime.class),
                eq(Status.ACTIVE),
                eq(Status.DORMANT)
        );
    }
}