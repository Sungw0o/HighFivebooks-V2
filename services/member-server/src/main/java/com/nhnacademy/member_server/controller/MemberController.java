package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.MemberApi;
import com.nhnacademy.member_server.dto.request.member.DormantRequest;
import com.nhnacademy.member_server.dto.request.member.MemberUpdateRequest;
import com.nhnacademy.member_server.dto.response.member.MemberResponse;
import com.nhnacademy.member_server.dto.response.member.SimpleMemberResponse;
import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.entity.member.Role;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.global.jwt.WebUtils;
import com.nhnacademy.member_server.service.member.AuthService;
import com.nhnacademy.member_server.service.member.EmailService;
import com.nhnacademy.member_server.service.member.MemberService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Slf4j
public class MemberController implements MemberApi {

    private final MemberService memberService;
    private final AuthService authService;
    private final EmailService emailService;

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMember(@RequestHeader(name = "X-User-ID") Long memberId){
        MemberResponse response = memberService.getMember(memberId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<MemberResponse> updateMember(@RequestHeader(name = "X-User-ID") Long memberId,
                                                       @RequestBody @Valid MemberUpdateRequest memberUpdateRequest) {
        MemberResponse memberResponse = memberService.updateMember(memberId, memberUpdateRequest);
        return ResponseEntity.ok(memberResponse);
    }


    @DeleteMapping("me/withdraw")
    public ResponseEntity<Void> withdraw(@RequestHeader(name = "X-User-ID") Long memberId,
                                         @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerHeader) {
        authService.logout(WebUtils.getToken(bearerHeader), memberId);
        memberService.withdraw(memberId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/birthday")
    public ResponseEntity<List<Long>> getBirthdayMemberIds(
            @RequestParam("month") int month
    ) {
        List<Long> memberIds = memberService.getBirthdayMemberIds(month);
        return ResponseEntity.ok(memberIds);
    }


    @PutMapping("/{member-id}/role")
    public ResponseEntity<String> updateMemberRole(
            @PathVariable("member-id") Long memberId,
            @RequestParam Role role
    ) {
        memberService.updateRole(memberId, role);
        return ResponseEntity.ok("회원(" + memberId + ")의 권한이 " + role + "로 변경되었습니다.");
    }

    @PostMapping("/list")
    public ResponseEntity<List<SimpleMemberResponse>> getMembersInfo(@RequestBody List<Long> memberIds) {
        List<SimpleMemberResponse> responseList = memberService.getMembersInfo(memberIds);
        return ResponseEntity.ok(responseList);
    }

    @PostMapping("/open/dormant/check")
    public ResponseEntity<Boolean> checkDormant(@RequestBody DormantRequest request) {
        memberService.checkDormantMember(request.getLoginId(), request.getEmail());
        return ResponseEntity.ok(true);
    }

    @PostMapping("/open/dormant/activate")
    public ResponseEntity<Void> activateDormant(@RequestBody @Valid DormantRequest request) {

        String rawCode = request.getAuthCode();
        if (rawCode != null) {
            rawCode = rawCode.trim();
        }

        boolean isVerified = emailService.verifyCode(request.getEmail(), rawCode, EmailType.ACTIVATE);

        if (!isVerified) {
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        memberService.activateDormantMember(request.getLoginId(), request.getEmail());
        return ResponseEntity.ok().build();
    }
}