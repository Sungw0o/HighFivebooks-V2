package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.MemberCouponDocs;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.request.UserCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.service.CouponService;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coupons") // [수정 1] 경로를 api/coupons -> api/member-coupons 로 변경
@RequiredArgsConstructor
public class MemberCouponController implements MemberCouponDocs {

    private final MemberCouponService memberCouponService;
    private final CouponService couponService;

    @Override
    @PostMapping("/issue") // [수정 2] 어노테이션 명시적 추가 (이하 동일)
    public ResponseEntity<Void> issueCoupon(@RequestHeader("X-USER-ID") Long memberId, @RequestBody UserCouponIssueRequestDto requestDto) {
        memberCouponService.issueCouponByUser(memberId, requestDto.getCouponId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @GetMapping("/members")
    public ResponseEntity<Page<MemberCouponResponseDto>> getCouponsByUserId(@RequestHeader("X-USER-ID") Long memberId, Pageable pageable) {
        Page<MemberCouponResponseDto> responseDtos = memberCouponService.findCouponByUserId(memberId, pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    @GetMapping("/templates")
    public ResponseEntity<Page<CouponResponseDto>> getIssuableCoupons(Pageable pageable) {
        Page<CouponResponseDto> responseDtos = couponService.findIssuableCoupons(pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    @GetMapping("/members/order")
    public ResponseEntity<List<MemberCouponResponseDto>> getUsableCoupons(@RequestHeader("X-USER-ID") Long memberId,
                                                                          @RequestParam(value = "bookIds", required = false) List<Long> bookIds,
                                                                          @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds) {
        List<MemberCouponResponseDto> responseDtos = memberCouponService.findUsableCoupons(memberId, bookIds, categoryIds);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    @PostMapping("/calculate")
    public ResponseEntity<CouponCalculationResponseDto> calculateCoupon(@RequestHeader("X-USER-ID") Long memberId,
                                                                        @Valid @RequestBody CouponCalculationRequestDto requestDto) {
        CouponCalculationResponseDto responseDto = memberCouponService.calculateDiscount(memberId, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @Override
    @PostMapping("/use")
    public ResponseEntity<Void> useCoupon(@RequestHeader("X-USER-ID") Long memberId, @RequestBody MemberCouponUseRequestDto requestDto) {
        log.info("============== [4. 쿠폰 서버 요청 수신] UserID: {}, CouponID: {}, OrderID: {} ==============",
                memberId, requestDto.getCouponId(), requestDto.getOrderId());
        memberCouponService.useCoupon(memberId, requestDto);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancelCouponUsage(@RequestHeader("X-USER-ID") Long memberId, @RequestBody MemberCouponCancelRequestDto requestDto) {
        memberCouponService.cancelCouponUsage(memberId, requestDto);
        return ResponseEntity.ok().build();
    }
}