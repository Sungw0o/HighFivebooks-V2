package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.point.PointAdminAdjustmentRequest;
import com.nhnacademy.member_server.dto.request.point.PointAdminPolicyRequest;
import com.nhnacademy.member_server.dto.response.point.PointAdminPolicyResponse;
import com.nhnacademy.member_server.dto.response.point.PointTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "8. Point Admin", description = "관리자용 포인트 정책 및 수동 조정 API")
public interface PointAdminApi {

    @Operation(summary = "포인트 정책 조회", description = "현재 적용 중인 최신 포인트 정책을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "포인트 정책이 설정되지 않았습니다"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<PointAdminPolicyResponse> getPolicy();

    @Operation(summary = "포인트 정책 변경", description = "새로운 포인트 적립 정책을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "정책 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<Void> updatePolicy(@RequestBody PointAdminPolicyRequest request);

    @Operation(summary = "포인트 수동 조정", description = "관리자가 회원의 포인트를 임의로 지급하거나 차감합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조정 성공 (변경된 잔액 반환)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (차감 시 잔액 부족 등)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<PointTransactionResponse> adjustmentMemberPoint(@RequestBody PointAdminAdjustmentRequest request);
}
