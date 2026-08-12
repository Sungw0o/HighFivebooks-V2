package com.nhnacademy.order_server.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("동일 Cancel을 두 번 호출해도 외부 취소와 상태 변경은 한 번만 수행한다")
    void cancel_DuplicateRequest_IsIdempotent() {
        Long orderId = 1L;
        Order waiting = Order.builder()
                .id(orderId).userId(100L).deliveryStatus(DeliveryStatus.PAYMENT_WAITING)
                .orderKey("order-key").build();
        waiting.addOrderItem(OrderItem.builder().bookId(10L).quantity(1).build());
        Order canceled = Order.builder().id(orderId).deliveryStatus(DeliveryStatus.CANCELED).build();
        given(orderRepository.findByIdWithItems(orderId))
                .willReturn(Optional.of(waiting), Optional.of(canceled));

        orderCancelService.cancelOrderTransactional(orderId);
        orderCancelService.cancelOrderTransactional(orderId);

        verify(bookClient).releaseHeldStock(anyList(), eq("order-key"));
        verify(orderStatusMutationService).markCanceled(orderId);
    }

    @Test
    @DisplayName("포인트 Cancel 실패 시 쿠폰·재고·상태 보상은 진행되지 않는다")
    void cancel_PointFailure_StopsRemainingCompensation() {
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId).userId(100L).deliveryStatus(DeliveryStatus.PAYMENT_WAITING)
                .orderKey("order-key").pointDiscount(1000).couponId(20L).build();
        order.addOrderItem(OrderItem.builder().bookId(10L).quantity(1).build());
        given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
        willThrow(new RuntimeException("member cancel failed")).given(memberClient).cancelPoint(any());

        assertThatThrownBy(() -> orderCancelService.cancelOrderTransactional(orderId))
                .isInstanceOf(RuntimeException.class).hasMessage("member cancel failed");

        verify(couponClient, never()).cancelCouponUsage(any(), any());
        verify(bookClient, never()).releaseHeldStock(anyList(), any());
        verify(orderStatusMutationService, never()).markCanceled(orderId);
    }

    @Test
    @DisplayName("결제 Cancel timeout 시 재고 복구와 주문 상태 변경은 진행되지 않는다")
    void cancel_PaymentTimeout_StopsStockCompensationAndStatusChange() {
        Long orderId = 1L;
        Order order = Order.builder()
                .id(orderId).userId(100L).deliveryStatus(DeliveryStatus.PREPARING)
                .paymentKey("pg-key").paymentAmount(10000).build();
        order.addOrderItem(OrderItem.builder().bookId(10L).quantity(1).build());
        given(orderRepository.findByIdWithItems(orderId)).willReturn(Optional.of(order));
        willThrow(new RuntimeException("Read timed out"))
                .given(paymentClient).cancelPayment(eq("pg-key"), any());

        assertThatThrownBy(() -> orderCancelService.cancelOrderTransactional(orderId))
                .isInstanceOf(RuntimeException.class).hasMessage("Read timed out");

        verify(bookClient, never()).restoreStock(anyList(), any());
        verify(orderStatusMutationService, never()).markCanceled(orderId);
    }
}
