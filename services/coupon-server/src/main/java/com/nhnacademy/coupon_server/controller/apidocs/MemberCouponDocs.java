package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.request.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "MemberCoupon", description = "사용자 전용 회원 쿠폰 관리 API")
public interface MemberCouponDocs {

    @Operation(summary = "쿠폰 발급 신청", description = "로그인한 사용자가 선착순 또는 일반 쿠폰 발급을 요청합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "쿠폰 발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (발급 기간 위반, 유효하지 않은 쿠폰 등)"),
            @ApiResponse(responseCode = "404", description = "대상 쿠폰을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 발급된 쿠폰이거나 재고가 소진됨")
    })
    @PostMapping("/issue")
    ResponseEntity<Void> issueCoupon(
            @Parameter(description = "회원 ID (X-USER-ID 헤더)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long memberId,
            @Valid @RequestBody UserCouponIssueRequestDto requestDto
    );

    @Operation(summary = "사용자 쿠폰 목록 조회", description = "사용자가 보유한 쿠폰 목록을 조회합니다. (페이징 적용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = MemberCouponResponseDto.class)))
    })
    @GetMapping("/members")
    ResponseEntity<Page<MemberCouponResponseDto>> getCouponsByUserId(
            @Parameter(name = "memberId", description = "회원 ID (X-USER-ID 헤더)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long memberId,
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(summary = "발급 가능한 쿠폰 목록 조회", description = "현재 발급 기간에 해당하여 사용자가 다운로드할 수 있는 쿠폰 템플릿 목록을 조회합니다.")
    @GetMapping("/templates")
    ResponseEntity<Page<CouponResponseDto>> getIssuableCoupons(
            @Parameter(hidden = true) Pageable pageable
    );

    @Operation(summary = "주문 시 적용 가능 쿠폰 조회", description = "주문서 작성 시 사용자가 보유한 쿠폰 중 사용 가능한(미사용, 유효기간 내) 쿠폰 목록을 조회합니다.")
    @GetMapping("/members/order")
    ResponseEntity<List<MemberCouponResponseDto>> getUsableCoupons(
            @Parameter(name = "memberId", description = "회원 ID (X-USER-ID 헤더)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long memberId,
            @RequestParam(value = "bookIds", required = false) List<Long> bookIds,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds
    );

    @Operation(summary = "쿠폰 할인 금액 계산", description = "주문 금액에 대해 특정 쿠폰을 적용했을 때의 할인 금액을 계산하고 유효성을 검증합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "계산 성공"),
            @ApiResponse(responseCode = "400", description = "최소 주문 금액 미달 또는 유효하지 않은 쿠폰")
    })
    @PostMapping("/calculate")
    ResponseEntity<CouponCalculationResponseDto> calculateCoupon(
            @Parameter(description = "회원 ID (X-USER-ID 헤더)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long memberId,
            @Valid @RequestBody CouponCalculationRequestDto requestDto
    );

    @Operation(summary = "쿠폰 사용 처리", description = "결제가 완료된 후 쿠폰 상태를 '사용됨(USED)'으로 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용 처리 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 유효하지 않은 쿠폰(만료, 이미 사용됨)"),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    @PostMapping("/use")
    ResponseEntity<Void> useCoupon(
            @Parameter(description = "회원 ID (X-USER-ID 헤더)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long memberId,
            @Valid @RequestBody MemberCouponUseRequestDto requestDto
    );

    @Operation(summary = "쿠폰 사용 취소", description = "주문 취소/환불 시 쿠폰 상태를 '사용 가능(ISSUED)'으로 복구합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "취소(복구) 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(이미 취소됨, 다른 주문 ID 등)"),
            @ApiResponse(responseCode = "404", description = "쿠폰을 찾을 수 없음")
    })
    @PostMapping("/cancel")
    ResponseEntity<Void> cancelCouponUsage(
            @Parameter(description = "회원 ID (X-USER-ID 헤더)", required = true, in = ParameterIn.HEADER, example = "1")
            @RequestHeader("X-USER-ID") Long memberId,
            @Valid @RequestBody MemberCouponCancelRequestDto requestDto
    );
}
