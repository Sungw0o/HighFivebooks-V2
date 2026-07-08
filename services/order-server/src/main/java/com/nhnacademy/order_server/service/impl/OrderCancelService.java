package com.nhnacademy.order_server.service.impl;

import com.nhnacademy.order_server.adapter.BookClient;
import com.nhnacademy.order_server.adapter.CouponClient;
import com.nhnacademy.order_server.adapter.MemberClient;
import com.nhnacademy.order_server.adapter.PaymentClient;
import com.nhnacademy.order_server.dto.OrderCancellationData;
import com.nhnacademy.order_server.dto.request.MemberCouponCancelRequest;
import com.nhnacademy.order_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.order_server.dto.request.PointTransactionRequest;
import com.nhnacademy.order_server.entity.Order;
import com.nhnacademy.order_server.entity.enums.DeliveryStatus;
import com.nhnacademy.order_server.exception.OrderErrorCode;
import com.nhnacademy.order_server.exception.OrderException;
import com.nhnacademy.order_server.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCancelService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;
    private final MemberClient memberClient;
    private final CouponClient couponClient;
    private final BookClient bookClient;
    private final OrderStatusMutationService orderStatusMutationService;

    public void cancelOrderTransactional(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
        OrderCancellationData data = OrderCancellationData.from(order);

        if (data.deliveryStatus() == DeliveryStatus.CANCELED) {
            return;
        }

        cancelPointReservation(data);
        cancelCouponUsage(data);

        if (data.deliveryStatus() == DeliveryStatus.PREPARING) {
            processPreparingOrderCancellation(data);
        } else {
            processPaymentWaitingOrderCancellation(data);
        }

        orderStatusMutationService.markCanceled(data.orderId());
    }

    private void cancelPointReservation(OrderCancellationData data) {
        if (data.pointDiscount() == null || data.pointDiscount() <= 0) {
            return;
        }

        memberClient.cancelPoint(PointTransactionRequest.builder()
                .memberId(data.userId())
                .amount(Long.valueOf(data.pointDiscount()))
                .orderId(data.orderId())
                .build());
    }

    private void cancelCouponUsage(OrderCancellationData data) {
        if (data.couponId() == null) {
            return;
        }

        couponClient.cancelCouponUsage(data.userId(),
                new MemberCouponCancelRequest(data.couponId(), data.orderId()));
    }

    private void processPreparingOrderCancellation(OrderCancellationData data) {
        if (data.paymentKey() != null) {
            paymentClient.cancelPayment(data.paymentKey(), new PaymentCancelRequest("cancel", data.paymentAmount()));
        }

        bookClient.restoreStock(data.restoreStockRequests(), data.orderId() + "-restore");
    }

    private void processPaymentWaitingOrderCancellation(OrderCancellationData data) {
        bookClient.releaseHeldStock(data.releaseBookIds(), data.orderKey());
    }
}
