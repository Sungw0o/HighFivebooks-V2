package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "MemberCouponAdmin", description = "관리자 전용 회원 쿠폰 관리 API")
public interface MemberCouponAdminDocs {
    @Operation(summary = "전체 회원 쿠폰 내역 조회", description = "발급된 모든 회원 쿠폰 내역을 페이징하여 조회합니다.")
    @GetMapping
    ResponseEntity<Page<MemberCouponResponseDto>> getMemberCoupons(
            @ParameterObject Pageable pageable
    );

    @Operation(summary = "쿠폰 수동 발급", description = "관리자가 특정 회원에게 쿠폰을 직접 발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "404", description = "쿠폰 템플릿을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 발급된 쿠폰임")
    })
    @PostMapping("/issue")
    ResponseEntity<Void> issueCouponByAdmin(
            @Valid @RequestBody MemberCouponIssueRequestDto requestDto
    );
}
