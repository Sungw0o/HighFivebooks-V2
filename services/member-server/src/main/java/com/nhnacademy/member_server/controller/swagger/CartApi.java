package com.nhnacademy.member_server.controller.swagger;

import com.nhnacademy.member_server.dto.request.cart.CartAddRequest;
import com.nhnacademy.member_server.dto.request.cart.CartItemUpdateRequest;
import com.nhnacademy.member_server.dto.response.cart.CartAddResponse;
import com.nhnacademy.member_server.dto.response.cart.CartListResponse;
import com.nhnacademy.member_server.dto.response.cart.CartUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "10. Cart", description = "장바구니(회원/비회원) 관련 API")
@RequestMapping("/api/cart")
public interface CartApi {

    @Operation(summary = "장바구니에 책 추가", description = "사용자의 장바구니를 찾아 책을 추가합니다. (비회원은 쿠키, 회원은 헤더 ID 사용)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "장바구니 담기 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (수량 오류 등)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 책")
    })
    @PostMapping("/items")
    ResponseEntity<CartAddResponse> addItemToCart(
            @RequestBody @Valid CartAddRequest request,
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId,
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
            HttpServletResponse httpResponse
    );

    @Operation(summary = "장바구니 내역 조회", description = "장바구니에 담긴 책의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    ResponseEntity<CartListResponse> getCartItems(
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId,
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId
    );

    @Operation(summary = "장바구니 전체 비우기", description = "장바구니의 모든 상품을 삭제합니다. (선택 삭제 혹은 전체 삭제)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "장바구니를 찾을 수 없음")
    })
    @DeleteMapping("/items")
    ResponseEntity<Void> deleteAllCartItem(
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId,
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
            @Parameter(description = "주문 여부 (true일 경우 주문과 관련된 로직 수행 가능)") @RequestParam(defaultValue = "false") boolean isOrder
    );

    @Operation(summary = "주문 후 장바구니 즉시 비우기", description = "주문이 완료된 후 장바구니(DB) 데이터를 즉시 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/items/immediately")
    ResponseEntity<Void> clearCartForOrder(
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId
    );

    @Operation(summary = "장바구니 수량 변경", description = "장바구니 상품의 수량을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 수량 (1 미만)"),
            @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음")
    })
    @PutMapping("/items")
    ResponseEntity<CartUpdateResponse> updateQuantity(
            @RequestBody @Valid CartItemUpdateRequest request,
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId
    );

    @Operation(summary = "장바구니 상품 단건 삭제", description = "특정 책 하나를 장바구니에서 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/items/{bookId}")
    ResponseEntity<Void> deleteOneItem(
            @Parameter(description = "삭제할 책 ID") @PathVariable Long bookId,
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId
    );

    @Operation(summary = "비회원 장바구니 병합", description = "로그인 시 비회원 장바구니 내역을 회원 장바구니로 병합합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "병합 성공")
    })
    @PostMapping("/merge")
    ResponseEntity<Void> mergeGuestCart(
            @Parameter(hidden = true) @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId,
            HttpServletResponse response
    );

    @Operation(summary = "비회원 장바구니 무시(삭제)", description = "로그인 시 비회원 장바구니 병합을 거절하고 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/guest")
    ResponseEntity<Void> ignoreGuestCart(
            @Parameter(hidden = true) @CookieValue(value = "guestCookie", required = false) String guestId,
            HttpServletResponse response
    );
}