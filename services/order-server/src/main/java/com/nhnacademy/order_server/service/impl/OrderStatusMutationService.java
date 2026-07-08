package com.nhnacademy.order_server.service.impl;

import com.nhnacademy.order_server.entity.Order;
import com.nhnacademy.order_server.entity.enums.DeliveryStatus;
import com.nhnacademy.order_server.exception.OrderErrorCode;
import com.nhnacademy.order_server.exception.OrderException;
import com.nhnacademy.order_server.dto.request.PointEarnRequest;
import com.nhnacademy.order_server.repository.OrderRepository;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderStatusMutationService {

    private final OrderRepository orderRepository;

    @Transactional
    public void markPaymentSuccess(Long orderId, String paymentKey, Integer expectedPaymentAmount) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getDeliveryStatus() != DeliveryStatus.PAYMENT_WAITING) {
            return;
        }

        if (!Objects.equals(order.getPaymentAmount(), expectedPaymentAmount)) {
            throw new OrderException(OrderErrorCode.INVALID_REQUEST);
        }

        order.updateStatus(DeliveryStatus.PREPARING);
        order.setPaymentKey(paymentKey);
    }

    @Transactional
    public void markCanceled(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getDeliveryStatus() == DeliveryStatus.CANCELED) {
            return;
        }

        order.updateStatus(DeliveryStatus.CANCELED);
    }

    @Transactional
    public Optional<PointEarnRequest> markPurchaseConfirmed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

        if (order.getDeliveryStatus() == DeliveryStatus.PURCHASE_CONFIRMED ||
                order.getDeliveryStatus() == DeliveryStatus.CANCELED ||
                order.getDeliveryStatus() == DeliveryStatus.RETURN_COMPLETED) {
            throw new OrderException(OrderErrorCode.ALREADY_PROCESSED);
        }

        if (order.getDelivery() != null && order.getDelivery().getActualCompletionDate() == null) {
            order.getDelivery().completeDelivery();
        }

        order.updateStatus(DeliveryStatus.PURCHASE_CONFIRMED);

        if (order.getUserId() == null || order.getPaymentAmount() == null) {
            return Optional.empty();
        }

        return Optional.of(PointEarnRequest.builder()
                .memberId(order.getUserId())
                .eventType("EARN_ORDER")
                .pureAmount(order.getPaymentAmount())
                .orderId(order.getId())
                .build());
    }
}
