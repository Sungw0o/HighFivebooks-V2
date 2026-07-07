package com.nhnacademy.coupon_server.controller;

import com.nhnacademy.coupon_server.controller.apidocs.CouponDocs;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponDocs {

    private final CouponService couponService;

    @Override
    public ResponseEntity<List<CouponResponseDto>> getBookCoupons(@PathVariable(name = "book-id") Long bookId,
                                                                  @RequestParam(name = "category-ids", required = false) List<Long> categoryIds,
                                                                  @RequestParam(name = "include-global", required = false, defaultValue = "true") boolean includeGlobal) {
        log.info("쿠폰 조회 요청 - BookId: {}, CategoryIds: {}, IncludeGlobal: {}", bookId, categoryIds, includeGlobal);

        List<CouponResponseDto> coupons;
        if (includeGlobal) {
            // 주문서 용 : 법용 나와야함.
            coupons = couponService.getCouponsForProduct(bookId, categoryIds);
        } else {
            // 도서 상세 페이지 용 : 범용 안나와야함.
            coupons = couponService.getBookSpecificCoupons(bookId, categoryIds);
        }
        return ResponseEntity.ok(coupons);
    }
}