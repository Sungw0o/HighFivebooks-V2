package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.request.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponPolicyResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Tag(name = "CouponPolicyAdmin", description = "쿠폰 서비스 CRUD API")
public interface CouponPolicyAdminDocs {

    @Operation(summary = "쿠폰 정책 생성", description = "새 쿠폰 정책을 생성합니다.")
    @ApiResponse(responseCode = "201", description = "쿠폰 정책 생성 성공")
    @PostMapping
    ResponseEntity<CouponPolicyResponseDto> createPolicy(
            @Valid @RequestBody(description = "쿠폰 정책 생성 요청 정보 (name)", required = true, content = @Content(schema = @Schema(implementation = CouponPolicyRequestDto.class)))
            @org.springframework.web.bind.annotation.RequestBody CouponPolicyRequestDto couponPolicyRequestDto
    );

    @Operation(summary = "쿠폰 정책리스트 조회", description = "쿠폰 정책 리스트를 보여줍니다.")
    @ApiResponse(responseCode = "200", description = "쿠폰 정책 리스트 조회 성공")
    @GetMapping
    ResponseEntity<List<CouponPolicyResponseDto>> getAllCouponPolicies();

    @Operation(summary = "쿠폰 정책 단건 조회", description = "쿠폰 정책 단건을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰 정책 단건 조회 성공",
                    content = @Content(schema = @Schema(implementation = CouponPolicyResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "쿠폰 정책 단건 조회 실패")
    })
    @GetMapping("/{couponPolicyId}")
    ResponseEntity<CouponPolicyResponseDto> getCouponPolicy(
            @Parameter(name = "couponPolicyId", description = "조회할 정책 ID", required = true, in = ParameterIn.PATH, example = "1")
            @PathVariable("couponPolicyId") Long couponPolicyId
    );

    @Operation(summary = "쿠폰 정책 비활성화", description = "쿠폰 정책을 비활성화 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "쿠폰 정책 비활성화 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 정책을 찾을 수 없음"),
    })
    @DeleteMapping("/{couponPolicyId}")
    ResponseEntity<CouponPolicyResponseDto> deleteCouponPolicy(
            @Parameter(name = "couponPolicyId", description = "비활성화 할 정책 ID", required = true, in = ParameterIn.PATH, example = "1")
            @PathVariable("couponPolicyId") Long couponPolicyId
    );
}
