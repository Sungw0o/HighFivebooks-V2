package com.nhnacademy.payment_server.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesResponse {
    private LocalDate date;
    private Long dailyTotalAmount;
    private Long dailyCount;
}