package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "7. Point User", description = "사용자용 포인트 잔액 및 이력 조회 API")
public interface PointUserApi {

    @Operation(summary = "내 포인트 잔액 조회", description = "현재 사용자의 포인트 잔액을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<PointBalanceResponse> getMyBalance(
            @Parameter(description = "회원 식별 ID")
            @RequestHeader("X-USER-ID") Long memberId
    );


    @Operation(summary = "내 포인트 이력 조회", description = "포인트 변동 내역을 최신순으로 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"), // Page 객체는 자동 매핑됨
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 "),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<Page<PointHistoryResponse>> getMyHistory(
            @Parameter(description = "회원 식별 ID")
            @RequestHeader("X-USER-ID") Long memberId,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "한 페이지당 개수", example = "10")
            @RequestParam(defaultValue = "10") int size
    );
}
