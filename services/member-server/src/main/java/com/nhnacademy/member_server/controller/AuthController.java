package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.AuthApi;
import com.nhnacademy.member_server.dto.request.member.LoginRequest;
import com.nhnacademy.member_server.dto.response.member.TokenDto;
import com.nhnacademy.member_server.global.jwt.WebUtils;
import com.nhnacademy.member_server.service.member.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody LoginRequest loginRequest) {
        TokenDto tokenDto = authService.loginUser(loginRequest.getLoginId(), loginRequest.getPassword());
        return ResponseEntity.ok(tokenDto);
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenDto> reissue(
            @RequestHeader("X-Refresh-Token") String refreshToken
    ) {
        TokenDto tokenDto = authService.reissue(refreshToken);
        return ResponseEntity.ok(tokenDto);
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "X-User-ID") Long memberId,
                                       @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {

        authService.logout(WebUtils.getToken(bearerToken), memberId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login/{provider}")
    public ResponseEntity<TokenDto> loginSocial(
            @PathVariable String provider,
            @RequestParam("code") String code
    ) {
        TokenDto tokenDto = authService.loginSocial(provider, code);

        return ResponseEntity.ok(tokenDto);
    }
}