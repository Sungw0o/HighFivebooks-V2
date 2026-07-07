package com.nhnacademy.payment_server.client;

import com.nhnacademy.payment_server.dto.response.OrderValidationInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "team5-order-server")
public interface OrderClient {
    @GetMapping("/api/orders/{orderKey}/payments")
    OrderValidationInfoResponse getOrderByKey(@PathVariable("orderKey") String orderKey);
}
