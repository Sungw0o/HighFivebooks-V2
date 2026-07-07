package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.request.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.request.CouponStatusRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "CouponAdmin", description = "관리자 전용 쿠폰 템플릿 관리 API")
public interface CouponAdminDocs {
    @Operation(summary = "쿠폰 템플릿 생성", description = "정책 기반으로 쿠폰 템플릿을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "쿠폰 템플릿 생성 성공", content = @Content(schema = @Schema(implementation = CouponResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터 - 유효성 검사 실패"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰 정책 ID")
    })
    @PostMapping
    ResponseEntity<CouponResponseDto> createCoupon(@Valid @RequestBody CouponRequestDto couponRequestDto);

    @Operation(summary = "쿠폰 템플릿 목록 조회", description = "생성된 모든 쿠폰 템플릿 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "쿠폰 템플릿 목록 조회 성공")
    @GetMapping
    ResponseEntity<List<CouponResponseDto>> findAllCoupons();

    @Operation(summary = "쿠폰 템플릿 상태 변경", description = "쿠폰 템플릿을 활성화(ACTIVE) 또는 비활성화(INACTIVE) 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태 값"),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    @PostMapping("/{couponId}/change-status")
    ResponseEntity<Void> updateCouponStatus(
            @Parameter(description = "쿠폰 ID", required = true) @PathVariable Long couponId,
            @Parameter(description = "변경할 상태 (ACTIVE, INACTIVE)", required = true) @Valid @RequestBody CouponStatusRequestDto requestDto
            );

}
