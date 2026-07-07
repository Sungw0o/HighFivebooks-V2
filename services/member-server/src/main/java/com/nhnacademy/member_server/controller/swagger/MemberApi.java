package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.member.DormantRequest;
import com.nhnacademy.member_server.dto.request.member.MemberUpdateRequest;
import com.nhnacademy.member_server.dto.response.member.MemberResponse;
import com.nhnacademy.member_server.dto.response.member.SimpleMemberResponse;
import com.nhnacademy.member_server.entity.member.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "4. Member", description = "회원 정보 관리")
public interface MemberApi {

    @Operation(summary = "내 정보 조회")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MemberResponse.class),
            examples = @ExampleObject(value = """
                    {
                      "name": "홍길동",
                      "email": "test@nhn.com",
                      "birthDate": "2000-01-01",
                      "phone": "010-1234-5678",
                      "status": "ACTIVE",
                      "gradeName": "GOLD"
                    }
                    """)))
    ResponseEntity<MemberResponse> getMember(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId);

    @Operation(summary = "내 정보 수정")
    @ApiResponse(responseCode = "200", description = "수정된 정보 반환", content = @Content(schema = @Schema(implementation = MemberResponse.class)))
    ResponseEntity<MemberResponse> updateMember(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @RequestBody MemberUpdateRequest memberUpdateRequest);

    @Operation(summary = "회원 탈퇴")
    @ApiResponse(responseCode = "200", description = "탈퇴 및 로그아웃 완료")
    ResponseEntity<Void> withdraw(
            @Parameter(hidden = true) @RequestHeader(name = "X-User-ID") Long memberId,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerHeader);

    @Operation(summary = "생일자 조회 (Batch용)")
    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = "[101, 105, 203, 500]")))
    ResponseEntity<List<Long>> getBirthdayMemberIds(
            @Parameter(description = "조회할 월") @RequestParam("month") int month);

    @Operation(summary = "회원 권한 변경 (Admin)")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "회원(100)의 권한이 ADMIN로 변경되었습니다.")))
    ResponseEntity<String> updateMemberRole(
            @PathVariable("member-id") Long memberId,
            @RequestParam Role role);

    @Operation(summary = "회원 리스트 정보 조회 (Bulk)")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SimpleMemberResponse.class),
            examples = @ExampleObject(value = """
                    [
                      { "memberId": 1, "name": "김철수" },
                      { "memberId": 2, "name": "이영희" }
                    ]
                    """)))
    ResponseEntity<List<SimpleMemberResponse>> getMembersInfo(@RequestBody List<Long> memberIds);

    @Operation(summary = "휴면 여부 확인")
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(value = "true")))
    ResponseEntity<Boolean> checkDormant(@RequestBody DormantRequest request);

    @Operation(summary = "휴면 계정 활성화")
    @ApiResponse(responseCode = "200", description = "활성화 완료")
    ResponseEntity<Void> activateDormant(@RequestBody DormantRequest request);
}