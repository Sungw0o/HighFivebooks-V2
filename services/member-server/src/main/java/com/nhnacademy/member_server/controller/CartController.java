package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.CartApi;
import com.nhnacademy.member_server.dto.request.cart.CartAddRequest;
import com.nhnacademy.member_server.dto.request.cart.CartItemUpdateRequest;
import com.nhnacademy.member_server.dto.response.cart.CartAddResponse;
import com.nhnacademy.member_server.dto.response.cart.CartListResponse;
import com.nhnacademy.member_server.dto.response.cart.CartUpdateResponse;
import com.nhnacademy.member_server.service.CartService;
import com.nhnacademy.member_server.utils.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController implements CartApi {
    private static final String GUEST_COOKIE_NAME = "guestCookie";
    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartAddResponse> addItemToCart(@RequestBody @Valid CartAddRequest request,
                                                         @CookieValue(value = GUEST_COOKIE_NAME, required = false) String guestId,
                                                         @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
                                                         HttpServletResponse httpResponse) {
        // 둘 다 존재하지 않으면 새로운 guest 장바구니 생성
        if (memberId == null && guestId == null) {
            guestId = UUID.randomUUID().toString();
            // 만료 시간 12시간 설정
            CookieUtils.addCookie(httpResponse, GUEST_COOKIE_NAME , guestId, 60 * 60 * 12);
        }

        CartAddResponse response = cartService.addToCart(request, memberId, guestId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 장바구니가 없으면 그냥 빈 리스트 반환
    @GetMapping
    public ResponseEntity<CartListResponse> getCartItems(@CookieValue(value = GUEST_COOKIE_NAME , required = false) String guestId,
                                                         @RequestHeader(name = "X-USER-ID", required = false) Long memberId){
        CartListResponse cartList = cartService.getCartItemList(memberId, guestId);

       return ResponseEntity.ok(cartList);
    }

    // 장바구니 비우기
    @DeleteMapping("/items")
    public ResponseEntity<Void> deleteAllCartItem(@CookieValue(value = GUEST_COOKIE_NAME , required = false) String guestId,
                                                  @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
                                                  @RequestParam(defaultValue = "false") boolean isOrder){
        cartService.deleteAllCartItem(memberId, guestId, isOrder);

        return ResponseEntity.noContent().build();
    }

    // 주문 완료 후 비우기 (DB 삭제)
    @DeleteMapping("/items/immediately")
    public ResponseEntity<Void> clearCartForOrder(@RequestHeader(name = "X-USER-ID", required = false) Long memberId) {

        cartService.deleteAllCartItemForOrder(memberId);

        return ResponseEntity.noContent().build();
    }

    // 수량 변경, 책의 아이디와 바뀔 수량은 request에 담겨서 넘어옴
    @PutMapping("/items")
    public ResponseEntity<CartUpdateResponse> updateQuantity(@RequestBody @Valid CartItemUpdateRequest request,
                                                             @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
                                                             @CookieValue(value = GUEST_COOKIE_NAME, required = false) String guestId) {
        CartUpdateResponse response = cartService.updateCartItemQuantity(memberId, guestId, request);
        return ResponseEntity.ok(response);
    }

    // 책 단건 삭제
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> deleteOneItem(@PathVariable Long bookId,
                                              @RequestHeader(name = "X-USER-ID", required = false) Long memberId,
                                              @CookieValue(value = GUEST_COOKIE_NAME, required = false) String guestId) {
        cartService.deleteCartItem(memberId, guestId, bookId);

        return ResponseEntity.noContent().build();
    }

    // 장바구니 합친다 했을때 예
    @PostMapping("/merge")
    public ResponseEntity<Void> mergeGuestCart(@RequestHeader(name = "X-USER-ID", required = false) Long memberId,
                                               @CookieValue(value = GUEST_COOKIE_NAME, required = false) String guestId,
                                               HttpServletResponse response){
        // 비회원 장바구니가 있으면 병합
        if(guestId != null){
            cartService.migrateGuestCart(guestId, memberId);
            CookieUtils.deleteCookie(response, GUEST_COOKIE_NAME);
        }

        return ResponseEntity.ok().build();
    }

    // 장바구니를 합치겠습니까? 했을 때 아니오
    @DeleteMapping("/guest")
    public ResponseEntity<Void> ignoreGuestCart(@CookieValue(value = GUEST_COOKIE_NAME, required = false) String guestId,
                                                HttpServletResponse response) {
        if (guestId != null) {
            // Redis 비회원 키 삭제
            cartService.deleteGuestCartOnly(guestId);

            // 쿠키 삭제
            CookieUtils.deleteCookie(response, GUEST_COOKIE_NAME);
        }
        return ResponseEntity.noContent().build();
    }
}
