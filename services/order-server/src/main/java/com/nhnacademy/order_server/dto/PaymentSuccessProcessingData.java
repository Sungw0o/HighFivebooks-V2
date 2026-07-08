package com.nhnacademy.order_server.dto;

import com.nhnacademy.order_server.entity.Order;
import java.util.Collections;
import java.util.List;

public record PaymentSuccessProcessingData(
        Long orderId,
        Long userId,
        Long couponId,
        Integer pointDiscount,
        String orderKey,
        Integer paymentAmount,
        List<Long> stockDeductionBookIds
) {

    public static PaymentSuccessProcessingData from(Order order) {
        List<Long> bookIds = order.getOrderItems().stream()
                .flatMap(item -> Collections.nCopies(item.getQuantity(), item.getBookId()).stream())
                .toList();

        return new PaymentSuccessProcessingData(
                order.getId(),
                order.getUserId(),
                order.getCouponId(),
                order.getPointDiscount(),
                order.getOrderKey(),
                order.getPaymentAmount(),
                bookIds
        );
    }
}
