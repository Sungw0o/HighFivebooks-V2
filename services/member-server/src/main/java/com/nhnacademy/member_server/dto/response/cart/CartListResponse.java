package com.nhnacademy.member_server.dto.response.cart;

import java.util.List;

public record CartListResponse(
        List<CartDetailResponse> items,
        long totalCartPrice, // 전체 총 주문 금액
        boolean hasGuestCart // 비회원 장바구니 유무 플래그
) {}