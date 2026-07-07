package com.nhnacademy.member_server.dto.response.point;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionResponse {
    @Schema(description = "포인트 사용/환불 된 유저 아이디", example = "1")
    private Long memberId;
    @Schema(description = "포인트 사용/환불 후 포인트 잔액", example = "2000")
    private Long currentPoint;
}
