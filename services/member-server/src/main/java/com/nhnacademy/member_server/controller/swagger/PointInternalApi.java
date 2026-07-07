package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionRequest;
import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointTransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "9. Point Internal", description = "내부 서버 통신용 포인트 API (주문/결제 연동)")
public interface PointInternalApi {

    @Operation(summary = "포인트 트랜잭션 생성 (통합)", description = "적립, 사용, 환불, 회수 등 모든 포인트 변동 사항을 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (잔액 부족, 유효하지 않은 타입 등)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<PointTransactionResponse> createTransaction(@RequestBody PointTransactionCreateRequest request);

    @Operation(summary = "[TCC] 포인트 사용 예약", description = "분산 트랜잭션 1단계: 주문 시 포인트를 차감 대기(HOLD) 상태로 만듭니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "예약 성공"),
            @ApiResponse(responseCode = "400", description = "예약 실패 (잔액 부족 등) - 주문 롤백 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<Void> reservePoint(@RequestBody PointTransactionRequest request);

    @Operation(summary = "[TCC] 포인트 사용 확정", description = "분산 트랜잭션 2단계: 예약된 포인트를 실제로 차감 확정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "404", description = "예약 내역을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<Void> confirmPoint(@RequestBody PointTransactionRequest request);

    @Operation(summary = "[TCC] 포인트 사용 취소", description = "분산 트랜잭션 보상: 예약된 포인트를 취소(롤백)합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    ResponseEntity<Void> cancelPoint(@RequestBody PointTransactionRequest request);

    @Operation(summary = "회원 포인트 잔액 조회", description = "타 서비스에서 특정 회원의 포인트 잔액을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원")
    })
    ResponseEntity<PointBalanceResponse> getPointBalance(
            @Parameter(description = "회원 ID", required = true)
            @PathVariable("memberId") Long memberId
    );
}