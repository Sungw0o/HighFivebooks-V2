package com.nhnacademy.payment_server.dto.response;

import com.nhnacademy.payment_server.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

    @Schema(description = "결제 수단 ID", example = "1")
    private Long id;

    @Schema(description = "결제 수단 코드 (시스템용)", example = "TOSS")
    private String name;

    @Schema(description = "결제 수단 별칭 (화면용)", example = "토스 페이먼츠")
    private String alias;

    @Schema(description = "활성화 여부", example = "true")
    private boolean isActive;

    public static PaymentMethodResponse from(PaymentMethod entity) {
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .alias(entity.getAlias())
                .isActive(entity.isActive())
                .build();
    }
}