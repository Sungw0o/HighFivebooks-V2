package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.EmailApi;
import com.nhnacademy.member_server.dto.request.member.EmailRequest;
import com.nhnacademy.member_server.dto.request.member.EmailVerifyRequest;
import com.nhnacademy.member_server.entity.member.EmailType;
import com.nhnacademy.member_server.service.member.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController implements EmailApi {

    private final EmailService emailService;

    @PostMapping("/signup")
    public ResponseEntity<Void> sendSignupCode(@RequestBody @Valid EmailRequest request) {
        emailService.sendVerificationCode(request.getEmail(), EmailType.SIGNUP);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> sendPasswordResetCode(@RequestBody @Valid EmailRequest request) {
        emailService.sendVerificationCode(request.getEmail(), EmailType.RESET_PASSWORD);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/find-id")
    public ResponseEntity<Void> sendFindIdCode(@RequestBody @Valid EmailRequest request) {
        emailService.sendVerificationCode(request.getEmail(), EmailType.FIND_ID);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/dormant/send")
    public ResponseEntity<Void> sendDormantCode(@RequestBody @Valid EmailRequest request) {
        emailService.sendVerificationCode(request.getEmail(), EmailType.ACTIVATE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyEmail(@RequestBody @Valid EmailVerifyRequest request) {

        boolean isVerified = emailService.verifyCode(
                request.getEmail(),
                request.getCode(),
                request.getType()
        );

        if (isVerified) {
            return ResponseEntity.ok("인증 성공");
        } else {
            return ResponseEntity.status(400).body("인증 실패");
        }
    }
}