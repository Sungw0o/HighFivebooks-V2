package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.member.LoginRequest;
import com.nhnacademy.member_server.dto.response.member.TokenDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "2. Auth", description = "로그인 및 토큰 관리")
public interface AuthApi {

    @Operation(summary = "일반 로그인", description = "ID/PW로 로그인하여 Access/Refresh Token을 발급받습니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = TokenDto.class),
            examples = @ExampleObject(value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiw...",
                      "refreshToken": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
                      "profileComplete": true
                    }
                    """)))
    ResponseEntity<TokenDto> login(@RequestBody LoginRequest loginRequest);

    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 갱신합니다.")
    @ApiResponse(responseCode = "200", description = "재발급 성공", content = @Content(schema = @Schema(implementation = TokenDto.class),
            examples = @ExampleObject(value = """
                    {
                      "accessToken": "new_access_token_value...",
                      "refreshToken": "new_refresh_token_value...",
                      "profileComplete": true
                    }
                    """)))
    ResponseEntity<TokenDto> reissue(
            @Parameter(name = "X-Refresh-Token", description = "Refresh Token", in = ParameterIn.HEADER, required = true)
            @RequestHeader("X-Refresh-Token") String refreshToken);

    @Operation(summary = "로그아웃", description = "서버 측 로그아웃 처리 (Redis 블랙리스트 등)")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    ResponseEntity<Void> logout(
            @Parameter(name = "X-User-ID", in = ParameterIn.HEADER, required = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken);

    @Operation(summary = "소셜 로그인", description = "OAuth 인증 코드로 로그인합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = TokenDto.class)))
    ResponseEntity<TokenDto> loginSocial(
            @Parameter(description = "Provider (payco)") @PathVariable String provider,
            @Parameter(description = "Auth Code") @RequestParam("code") String code);
}