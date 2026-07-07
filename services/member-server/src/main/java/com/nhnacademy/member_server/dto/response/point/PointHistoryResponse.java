package com.nhnacademy.member_server.dto.response.point;

import com.nhnacademy.member_server.entity.point.PointHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponse {
    @Schema(description = "이력 ID (정렬용)", example = "1")
    private Long id;

    @Schema(description = "포인트 변동 (적립 +, 사용 -)", example = "+5000")
    private Long amount;

    @Schema(description = "포인트 변동 사유", example = "상품 구매 적립 (주문번호: 1)")
    private String description;

    @Schema(description = "포인트 변동 후 잔액", example = "5500")
    private Long currentPoint;

    @Schema(description = "포인트 변동 일시", example = "2025-11-26T14:30:00")
    private LocalDateTime transactionDate;

    @Schema(description = "관련 주문 번호 (없으면 null)", example = "1")
    private Long orderId;

    public static PointHistoryResponse from(PointHistory entity) {
        return PointHistoryResponse.builder()
                .id(entity.getId())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .currentPoint(entity.getPointBalance())
                .transactionDate(entity.getCreatedAt())
                .orderId(entity.getOrderId())
                .build();
    }
}
