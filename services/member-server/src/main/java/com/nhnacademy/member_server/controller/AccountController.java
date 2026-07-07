package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.AccountApi;
import com.nhnacademy.member_server.dto.request.member.EmailVerifyRequest;
import com.nhnacademy.member_server.dto.request.member.MemberCreateRequest;
import com.nhnacademy.member_server.dto.request.member.PasswordResetRequest;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.member.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController implements AccountApi {

    private final AuthService authService;
    private final MemberRepository memberRepository;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid MemberCreateRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/check-id")
    public ResponseEntity<Boolean> checkId(@RequestParam("loginId") String loginId) {
        return ResponseEntity.ok(memberRepository.existsByLoginId(loginId));
    }

    @GetMapping("/exists/login-id/{loginId}")
    public ResponseEntity<Boolean> checkLoginId(@PathVariable String loginId) {
        boolean exists = memberRepository.existsByLoginId(loginId);
        return ResponseEntity.ok(exists);
    }

    @PostMapping("/find/id/verify")
    public ResponseEntity<String> findId(@RequestBody @Valid EmailVerifyRequest request) {
        String maskedId = authService.findLoginIdByEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(maskedId);
    }

    @PostMapping("/find/password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }


}