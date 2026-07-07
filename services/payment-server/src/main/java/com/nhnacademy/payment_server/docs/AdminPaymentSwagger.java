package com.nhnacademy.payment_server.docs;

import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.dto.response.PaymentStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin Payment API", description = "관리자용 결제 통계 API")
public interface AdminPaymentSwagger {

    @Operation(
            summary = "전체 매출 요약",
            description = "총 매출 금액, 취소 금액, 결제/취소 건수 등을 조회합니다."
    )
    ResponseEntity<PaymentStatsResponse> getSummary();

    @Operation(
            summary = "일별 매출 차트 데이터",
            description = "특정 기간의 일별 매출 합계를 조회합니다. 날짜를 지정하지 않으면 최근 7일 기준입니다."
    )
    ResponseEntity<List<DailySalesResponse>> getDailyStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    );
}
