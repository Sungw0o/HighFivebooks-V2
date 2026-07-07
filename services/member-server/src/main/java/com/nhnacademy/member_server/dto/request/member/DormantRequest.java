package com.nhnacademy.member_server.dto.request.member;

import com.nhnacademy.member_server.entity.member.EmailType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DormantRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "인증번호는 필수입니다.")
    private String authCode;

    private EmailType type;
}