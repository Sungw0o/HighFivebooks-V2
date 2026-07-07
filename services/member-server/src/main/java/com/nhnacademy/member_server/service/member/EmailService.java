package com.nhnacademy.member_server.service.member;

import com.nhnacademy.member_server.entity.member.EmailType;

public interface EmailService {
    void sendVerificationCode(String email, EmailType type);

    boolean verifyCode(String email, String inputCode, EmailType type);

}