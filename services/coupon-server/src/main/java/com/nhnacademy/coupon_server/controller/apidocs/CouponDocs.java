package com.nhnacademy.coupon_server.controller.apidocs;

import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Coupon", description = "공용 쿠폰 조회 API")
public interface CouponDocs {

    @Operation(summary = "도서별 적용 가능 쿠폰 조회", description = "특정 도서 상세 페이지에서 다운로드 가능한 쿠폰 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "쿠폰 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CouponResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "도서를 찾을 수 없음") // 필요 시 추가
    })
    @GetMapping("/books/{book-id}")
    ResponseEntity<List<CouponResponseDto>> getBookCoupons(
            @Parameter(name = "book-id", description = "도서 ID", required = true, in = ParameterIn.PATH, example = "1")
            @PathVariable("book-id") Long bookId,
            @RequestParam(name = "categoryIds", required = false) List<Long> categoryIds,
            @RequestParam(name = "include-global", required = false, defaultValue = "true") boolean includeGlobal
    );
}