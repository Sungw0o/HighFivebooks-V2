package com.nhnacademy.member_server.scheduler;

import com.nhnacademy.member_server.entity.member.Status;
import com.nhnacademy.member_server.repository.MemberRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DormantMemberScheduler {

    private final MemberRepository memberRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @SchedulerLock(
            name = "DormantMemberScheduler_processDormantMembers",
            lockAtMostFor = "5m",
            lockAtLeastFor = "30s"
    )
    @Transactional
    public void processDormantMembers() {
        log.info("[Scheduler] 휴면 회원 전환 배치 시작");

        LocalDateTime cutOffDate = LocalDateTime.now().minusMonths(3);

        int updatedCount = memberRepository.bulkUpdateDormantMembers(cutOffDate, Status.ACTIVE, Status.DORMANT);

        log.info("총 {}명의 회원이 휴면 상태로 전환되었습니다.", updatedCount);
    }
}