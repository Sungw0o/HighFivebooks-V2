package com.nhnacademy.member_server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.member_server.dto.request.cart.CartAddRequest;
import com.nhnacademy.member_server.dto.request.cart.CartItemUpdateRequest;
import com.nhnacademy.member_server.dto.response.cart.CartAddResponse;
import com.nhnacademy.member_server.dto.response.cart.CartListResponse;
import com.nhnacademy.member_server.dto.response.cart.CartUpdateResponse;
import com.nhnacademy.member_server.service.CartService;
import jakarta.servlet.http.Cookie;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CartService cartService;

    // --- [POST /api/cart/items] ---

    @Test
    @DisplayName("장바구니 담기 - 비회원 & 쿠키 없음 -> 새 쿠키(UUID) 생성")
    void addItemToCart_GuestNoCookie() throws Exception {
        CartAddRequest request = new CartAddRequest(1L, 1);
        CartAddResponse response = new CartAddResponse("key", 1L, 1);
        given(cartService.addToCart(any(), isNull(), anyString())).willReturn(response);

        mockMvc.perform(post("/api/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("guestCookie"));
    }

    @Test
    @DisplayName("장바구니 담기 - 회원 (X-USER-ID 존재) -> 쿠키 생성 안함")
    void addItemToCart_Member() throws Exception {
        CartAddRequest request = new CartAddRequest(1L, 1);
        CartAddResponse response = new CartAddResponse("key", 1L, 1);
        given(cartService.addToCart(any(), eq(1L), any())).willReturn(response);

        mockMvc.perform(post("/api/cart/items")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().doesNotExist("guestCookie")); // 쿠키 생성 로직 건너뜀
    }

    // --- [GET /api/cart] ---

    @Test
    @DisplayName("장바구니 조회")
    void getCartItems() throws Exception {
        CartListResponse response = new CartListResponse(Collections.emptyList(), 0L, false);
        given(cartService.getCartItemList(eq(1L), any())).willReturn(response);

        mockMvc.perform(get("/api/cart")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk());
    }

    // --- [DELETE /api/cart/items] ---

    @Test
    @DisplayName("장바구니 비우기")
    void deleteAllCartItem() throws Exception {
        mockMvc.perform(delete("/api/cart/items")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(cartService).deleteAllCartItem(1L, null, false);
    }

    @Test
    @DisplayName("주문 요청으로 장바구니 비우기")
    void deleteAllCartItemForOrder() throws Exception{
        mockMvc.perform(delete("/api/cart/items/immediately")
                .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(cartService).deleteAllCartItemForOrder(1L);
    }

    // --- [PUT /api/cart/items] ---

    @Test
    @DisplayName("수량 변경")
    void updateQuantity() throws Exception {
        CartItemUpdateRequest request = new CartItemUpdateRequest(1L, 5);
        CartUpdateResponse response = new CartUpdateResponse("key", 1L, 5);
        given(cartService.updateCartItemQuantity(eq(1L), any(), any())).willReturn(response);

        mockMvc.perform(put("/api/cart/items")
                        .header("X-USER-ID", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // --- [DELETE /api/cart/items/{bookId}] ---

    @Test
    @DisplayName("단건 삭제")
    void deleteOneItem() throws Exception {
        mockMvc.perform(delete("/api/cart/items/{bookId}", 100L)
                        .header("X-USER-ID", 1L))
                .andExpect(status().isNoContent());

        verify(cartService).deleteCartItem(1L, null, 100L);
    }

    // --- [POST /api/cart/merge] ---

    @Test
    @DisplayName("장바구니 병합 - 쿠키 있음 -> 병합 실행 및 쿠키 삭제")
    void mergeGuestCart_WithCookie() throws Exception {
        mockMvc.perform(post("/api/cart/merge")
                        .header("X-USER-ID", 1L)
                        .cookie(new Cookie("guestCookie", "guest-123")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("guestCookie", 0)); // 삭제 확인

        verify(cartService).migrateGuestCart("guest-123", 1L);
    }

    @Test
    @DisplayName("장바구니 병합 - 쿠키 없음 -> 병합 안함")
    void mergeGuestCart_NoCookie() throws Exception {
        mockMvc.perform(post("/api/cart/merge")
                        .header("X-USER-ID", 1L))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("guestCookie")); // 삭제 로직 실행 안됨

        verifyNoInteractions(cartService); // 호출 안됨
    }

    // --- [DELETE /api/cart/guest] ---

    @Test
    @DisplayName("게스트 장바구니 무시 - 쿠키 있음 -> 삭제 실행")
    void ignoreGuestCart_WithCookie() throws Exception {
        mockMvc.perform(delete("/api/cart/guest")
                        .cookie(new Cookie("guestCookie", "guest-123")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("guestCookie", 0));

        verify(cartService).deleteGuestCartOnly("guest-123");
    }

    @Test
    @DisplayName("게스트 장바구니 무시 - 쿠키 없음 -> 실행 안함")
    void ignoreGuestCart_NoCookie() throws Exception {
        mockMvc.perform(delete("/api/cart/guest"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().doesNotExist("guestCookie"));

        verifyNoInteractions(cartService);
    }
}