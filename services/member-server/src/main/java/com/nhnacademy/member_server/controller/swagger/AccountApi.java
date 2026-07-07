package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.member.EmailVerifyRequest;
import com.nhnacademy.member_server.dto.request.member.MemberCreateRequest;
import com.nhnacademy.member_server.dto.request.member.PasswordResetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "1. Account", description = "회원 가입, ID/PW 찾기 등 (비로그인)")
public interface AccountApi {

    @Operation(summary = "회원 가입", description = "새로운 회원을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 유효성 실패", content = @Content(examples = @ExampleObject(value = "{\"message\": \"비밀번호는 8~20자의 영문...\"}")))
    })
    ResponseEntity<Void> signup(@RequestBody MemberCreateRequest request);

    @Operation(summary = "아이디 중복 확인 (Query)", description = "로그인 ID 사용 가능 여부를 확인합니다.")
    @ApiResponse(responseCode = "200", description = "확인 성공 (true: 중복됨, false: 사용가능)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "true")))
    ResponseEntity<Boolean> checkId(@Parameter(description = "확인할 아이디") @RequestParam("loginId") String loginId);

    @Operation(summary = "아이디 중복 확인 (Path)", description = "경로 변수로 ID 중복을 확인합니다.")
    @ApiResponse(responseCode = "200", description = "확인 성공", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "false")))
    ResponseEntity<Boolean> checkLoginId(@Parameter(description = "확인할 아이디") @PathVariable String loginId);

    @Operation(summary = "아이디 찾기 (인증 완료)", description = "이메일 인증 후 마스킹된 아이디를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "nhn*****")))
    ResponseEntity<String> findId(@RequestBody EmailVerifyRequest request);

    @Operation(summary = "비밀번호 재설정", description = "인증된 사용자의 비밀번호를 변경합니다.")
    @ApiResponse(responseCode = "200", description = "변경 성공")
    ResponseEntity<Void> resetPassword(@RequestBody PasswordResetRequest request);
}