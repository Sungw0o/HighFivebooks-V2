package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.MemberCouponAdminDocs;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons/admin/member-coupons")
@RequiredArgsConstructor
public class MemberCouponAdminController implements MemberCouponAdminDocs {

    private final MemberCouponService memberCouponService;

    @Override
    public ResponseEntity<Page<MemberCouponResponseDto>> getMemberCoupons(@PageableDefault(size = 20) Pageable pageable) {
        Page<MemberCouponResponseDto> responseDtos = memberCouponService.findAll(pageable);
        return ResponseEntity.ok(responseDtos);
    }

    @Override
    public ResponseEntity<Void> issueCouponByAdmin(@Valid @RequestBody MemberCouponIssueRequestDto requestDto) {
        memberCouponService.issueCouponByAdmin(requestDto);
        return ResponseEntity.ok().build();
    }
}
