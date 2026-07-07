package com.nhnacademy.member_server.service.member;

import com.nhnacademy.member_server.dto.request.member.MemberCreateRequest;
import com.nhnacademy.member_server.dto.request.member.PasswordResetRequest;
import com.nhnacademy.member_server.dto.response.member.TokenDto;

public interface AuthService {
    TokenDto loginUser(String loginId, String password);

    void signup(MemberCreateRequest request);

    TokenDto reissue(String refreshToken);

    void logout(String accessToken, Long userId);

    TokenDto loginSocial(String provider, String code);

    String findLoginIdByEmail(String email, String code);

    void resetPassword(PasswordResetRequest request);
}
