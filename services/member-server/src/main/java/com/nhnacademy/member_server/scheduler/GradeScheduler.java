package com.nhnacademy.member_server.scheduler;

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GradeScheduler {

    private final MemberRepository memberRepository;
    private final GradeRepository gradeRepository;
    private final OrderFeignClient orderClient;

    @Autowired
    @Lazy
    private GradeScheduler self;

    @Scheduled(cron = "0 0 0 1 * *")
    @SchedulerLock(
            name = "gradeUpdateScheduler",
            lockAtMostFor = "1h",
            lockAtLeastFor = "30s"
    )
    public void updateMemberGrades() {
        log.info("[Scheduler] 회원 등급 산정 시작");

        LocalDateTime since = LocalDateTime.now().minusMonths(3);

        List<Grade> allGrades = gradeRepository.findAll();

        // [수정 2] 등급 정책이 없는 경우 방어 로직 (IndexOutOfBoundsException 방지)
        if (allGrades.isEmpty()) {
            log.error("[Scheduler] 등급 정책(Grade) 데이터가 없습니다. 스케줄러를 종료합니다.");
            return;
        }

        allGrades.sort((g1, g2) -> Integer.compare(g2.getMin(), g1.getMin()));

        int page = 0;
        int size = 1000;
        int totalUpdatedCount = 0;

        while (true) {
            Page<Member> memberPage = memberRepository.findAllByStatus(Status.ACTIVE, PageRequest.of(page, size));

            if (!memberPage.hasContent()) {
                break;
            }

            List<Member> members = memberPage.getContent();
            List<Long> memberIds = members.stream().map(Member::getId).toList();

            Map<Long, Long> orderStats = Collections.emptyMap();
            try {
                log.info("주문 서버로 요청 보냄: 회원 IDs = {}", memberIds);
                ResponseEntity<Map<Long, Long>> response = orderClient.getBulkTotalAmounts(memberIds, since);

                if (response.getBody() != null) {
                    orderStats = response.getBody();
                }
                log.info("주문 서버 응답 결과: {}", orderStats);

            } catch (Exception e) {
                log.error("[Error] 주문 서버 통신 중 오류 발생 (Page: {}). 해당 배치는 스킵합니다.", page, e);
                page++;
                continue;
            }

            // [수정 3] self를 통해 호출해야 @Transactional(REQUIRES_NEW)가 작동함
            totalUpdatedCount += self.updateBatch(members, orderStats, allGrades);

            page++;
        }

        log.info("[Scheduler] 회원 등급 산정 완료. 총 {}명 등급 변경됨.", totalUpdatedCount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int updateBatch(List<Member> members, Map<Long, Long> orderStats, List<Grade> sortedGrades) {
        int count = 0;
        List<Member> changedMembers = new ArrayList<>();

        for (Member member : members) {
            Long amount = orderStats.getOrDefault(member.getId(), 0L);
            Grade newGrade = matchGrade(amount, sortedGrades);

            if (!member.getGrade().getId().equals(newGrade.getId())) {
                log.info("회원 {} 등급 변경: {} -> {} (실적: {})",
                        member.getId(), member.getGrade().getGradeName(), newGrade.getGradeName(), amount);

                member.setGrade(newGrade);
                changedMembers.add(member);
                count++;
            }
        }

        if (!changedMembers.isEmpty()) {
            memberRepository.saveAll(changedMembers);
        }

        return count;
    }

    private Grade matchGrade(Long amount, List<Grade> sortedGrades) {
        for (Grade grade : sortedGrades) {
            if (amount >= grade.getMin()) {
                return grade;
            }
        }
        return sortedGrades.getLast();
    }
}