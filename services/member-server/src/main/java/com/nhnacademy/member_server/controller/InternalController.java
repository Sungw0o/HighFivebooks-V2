package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.InternalApi;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.scheduler.GradeScheduler;
import com.nhnacademy.member_server.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController implements InternalApi {

    private final MemberService memberService;
    private final GradeScheduler gradeScheduler;

    @PutMapping("/{member-id}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable("member-id") Long memberId,
            @RequestParam Role role
    ) {
        memberService.updateRole(memberId, role);
        return ResponseEntity.ok("회원(" + memberId + ")의 권한이 " + role + "로 변경되었습니다.");
    }

    @PostMapping("/grades/calculate")
    public ResponseEntity<String> forceCalculateGrades() {
        gradeScheduler.updateMemberGrades();
        return ResponseEntity.ok("등급 산정 스케줄러 강제 실행 완료");
    }
}
