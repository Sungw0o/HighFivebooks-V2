package com.nhnacademy.payment_server.controller;

import com.nhnacademy.payment_server.docs.AdminPaymentSwagger;
import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.dto.response.PaymentStatsResponse;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.service.PaymentStatService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/admin/stats")
@RequiredArgsConstructor
public class AdminPaymentController implements AdminPaymentSwagger {

    private final PaymentStatService paymentStatService;

    @GetMapping("/summary")
    @Override
    public ResponseEntity<PaymentStatsResponse> getSummary() {
        return ResponseEntity.ok(paymentStatService.getTotalStats());
    }

    @GetMapping("/daily")
    @Override
    public ResponseEntity<List<DailySalesResponse>> getDailyStats(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) {
            endDate = LocalDate.now();
        }

        if (startDate == null) {
            startDate = endDate.minusDays(6);  // 날짜 파라미터 없으면 최근 7일
        }

        if (startDate.isAfter(endDate)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        return ResponseEntity.ok(
                paymentStatService.getDailyStats(startDate.atStartOfDay(), endDate.atTime(LocalTime.MAX))
        );
    }
}