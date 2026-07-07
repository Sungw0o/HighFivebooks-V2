package com.nhnacademy.member_server.dto.request.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemUpdateRequest(@NotNull Long bookId,
                                    @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
                                    @Max(value = 100, message = "최대 주문 수량을 초과했습니다.")
                                    Integer quantity) {}

