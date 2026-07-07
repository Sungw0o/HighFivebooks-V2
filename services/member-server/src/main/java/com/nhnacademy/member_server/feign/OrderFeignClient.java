package com.nhnacademy.member_server.feign;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "TEAM5-ORDER-SERVER")
public interface OrderFeignClient {

    @PostMapping("/api/internal/orders/users/bulk-total-amount")
    ResponseEntity<Map<Long, Long>> getBulkTotalAmounts(
            @RequestBody List<Long> userIds,
            @RequestParam("since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since
    );
}