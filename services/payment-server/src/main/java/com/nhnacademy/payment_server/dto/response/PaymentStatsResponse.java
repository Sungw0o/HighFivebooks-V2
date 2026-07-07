package com.nhnacademy.payment_server.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsResponse {
    private Long totalSalesAmount;    // 총 매출액 (결제 완료 금액)
    private Long totalCancelAmount;   // 총 환불액 (취소된 금액)
    private Long netSalesAmount;      // 순 매출액 (총 매출 - 총 환불)
    private Long totalTransactionCount; // 총 결제 건수
    private Long successCount;        // 성공 건수
    private Long cancelCount;         // 취소 건수
}