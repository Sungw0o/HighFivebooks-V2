package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.entity.member.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "6. Internal", description = "서버 간 내부 호출용")
public interface InternalApi {

    @Operation(summary = "회원 권한 변경 (Internal)")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "회원(100)의 권한이 USER로 변경되었습니다.")))
    ResponseEntity<String> updateMemberRole(
            @PathVariable("member-id") Long memberId,
            @RequestParam Role role);

    @Operation(summary = "등급 산정 강제 실행")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "등급 산정 스케줄러 강제 실행 완료")))
    ResponseEntity<String> forceCalculateGrades();
}