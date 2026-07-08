package com.nhnacademy.order_server.dto;

import com.nhnacademy.order_server.dto.request.StockRequest;
import com.nhnacademy.order_server.entity.Order;
import com.nhnacademy.order_server.entity.OrderItem;
import com.nhnacademy.order_server.entity.enums.DeliveryStatus;
import java.util.List;

public record OrderCancellationData(
        Long orderId,
        Long userId,
        DeliveryStatus deliveryStatus,
        String paymentKey,
        Integer paymentAmount,
        Long couponId,
        Integer pointDiscount,
        String orderKey,
        List<Long> releaseBookIds,
        List<StockRequest> restoreStockRequests
) {

    public static OrderCancellationData from(Order order) {
        return new OrderCancellationData(
                order.getId(),
                order.getUserId(),
                order.getDeliveryStatus(),
                order.getPaymentKey(),
                order.getPaymentAmount(),
                order.getCouponId(),
                order.getPointDiscount(),
                order.getOrderKey(),
                order.getOrderItems().stream()
                        .map(OrderItem::getBookId)
                        .toList(),
                order.getOrderItems().stream()
                        .map(item -> new StockRequest(item.getBookId(), item.getQuantity()))
                        .toList()
        );
    }
}
