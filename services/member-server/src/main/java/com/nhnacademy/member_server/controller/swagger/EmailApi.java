package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.member.EmailRequest;
import com.nhnacademy.member_server.dto.request.member.EmailVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "5. Email", description = "이메일 인증")
public interface EmailApi {

    @Operation(summary = "회원가입 인증 메일 발송")
    @ApiResponse(responseCode = "200", description = "전송 완료")
    ResponseEntity<Void> sendSignupCode(@RequestBody EmailRequest request);

    @Operation(summary = "비밀번호 재설정 인증 메일 발송")
    @ApiResponse(responseCode = "200", description = "전송 완료")
    ResponseEntity<Void> sendPasswordResetCode(@RequestBody EmailRequest request);

    @Operation(summary = "아이디 찾기 인증 메일 발송")
    @ApiResponse(responseCode = "200", description = "전송 완료")
    ResponseEntity<Void> sendFindIdCode(@RequestBody EmailRequest request);

    @Operation(summary = "휴면 해제 인증 메일 발송")
    @ApiResponse(responseCode = "200", description = "전송 완료")
    ResponseEntity<Void> sendDormantCode(@RequestBody EmailRequest request);

    @Operation(summary = "이메일 코드 검증")
    @ApiResponse(responseCode = "200", description = "검증 성공", content = @Content(examples = @ExampleObject(value = "인증 성공")))
    @ApiResponse(responseCode = "400", description = "검증 실패", content = @Content(examples = @ExampleObject(value = "인증 실패")))
    ResponseEntity<String> verifyEmail(@RequestBody EmailVerifyRequest request);
}