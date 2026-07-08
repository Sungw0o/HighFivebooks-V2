package com.nhnacademy.order_server.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nhnacademy.order_server.adapter.BookClient;
import com.nhnacademy.order_server.adapter.CouponClient;
import com.nhnacademy.order_server.adapter.MemberClient;
import com.nhnacademy.order_server.adapter.PaymentClient;
import com.nhnacademy.order_server.entity.Order;
import com.nhnacademy.order_server.entity.OrderItem;
import com.nhnacademy.order_server.entity.enums.DeliveryStatus;
import com.nhnacademy.order_server.repository.OrderRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    @InjectMocks
    private OrderCancelService orderCancelService;

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentClient paymentClient;
    @Mock private MemberClient memberClient;
    @Mock private CouponClient couponClient;
    @Mock private BookClient bookClient;
    @Mock private OrderStatusMutationService orderStatusMutationService;

    @Test
    @DisplayName("PREPARING 주문 취소는 결제 취소와 재고 복구 후 상태를 변경한다")
    void cancel_Preparing() {
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .userId(100L)
                .deliveryStatus(DeliveryStatus.PREPARING)
                .paymentKey("pg_key")
                .paymentAmount(10000)
                .build();
        order.addOrderItem(OrderItem.builder().bookId(10L).quantity(1).build());

        given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));

        orderCancelService.cancelOrderTransactional(orderId);

        verify(paymentClient).cancelPayment(eq("pg_key"), any());
        verify(bookClient).restoreStock(anyList(), any());
        verify(orderStatusMutationService).markCanceled(orderId);
    }

    @Test
    @DisplayName("PAYMENT_WAITING 주문 취소는 선점 재고 해제 후 상태를 변경한다")
    void cancel_PaymentWaiting() {
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId)
                .userId(100L)
                .deliveryStatus(DeliveryStatus.PAYMENT_WAITING)
                .orderKey("key")
                .build();
        order.addOrderItem(OrderItem.builder().bookId(10L).quantity(1).build());

        given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));

        orderCancelService.cancelOrderTransactional(orderId);

        verify(bookClient).releaseHeldStock(anyList(), eq("key"));
        verify(orderStatusMutationService).markCanceled(orderId);
    }
}
