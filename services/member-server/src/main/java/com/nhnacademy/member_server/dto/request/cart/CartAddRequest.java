package com.nhnacademy.member_server.dto.request.cart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartAddRequest(
        @NotNull(message = "책 ID는 필수입니다.")
        Long bookId,

        @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
        @Max(value = 100, message = "한 번에 최대 100권까지만 담을 수 있습니다.") // 정책에 따라 추가
        int quantity
) {}