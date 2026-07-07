package com.nhnacademy.member_server.dto.request.point;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointAdminAdjustmentRequest {
    @Schema(description = "조정 대상 회원 ID", example = "1")
    @NotNull(message = "회원 ID는 필수입니다")
    private Long memberId;

    @Schema(description = "조정 금액 (양수 적립, 음수 차감 처리)", example = "300000")
    @NotNull(message = "조정 금액은 필수입니다")
    private Long amount;

    @Schema(description = "조정 사유", example = "특별 이벤트 포인트 적립")
    @NotBlank(message = "조정 사유는 필수입니다")
    private String reason;
}
