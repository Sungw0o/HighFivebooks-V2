package com.nhnacademy.member_server.dto.request.point;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointAdminPolicyRequest {
    @NotNull(message = "회원가입 적립금은 필수입니다")
    @Min(0)
    @Schema(description = "회원가입 적립금", example = "5000")
    private Integer signupPoint;

    @NotNull(message = "리뷰 적립금은 필수입니다")
    @Min(0)
    @Schema(description = "일반 리뷰 적립금", example = "200")
    private Integer reviewPoint;

    @NotNull(message = "사진 리뷰 적립금은 필수입니다")
    @Min(0)
    @Schema(description = "사진 리뷰 적립금", example = "500")
    private Integer photoPoint;
}
