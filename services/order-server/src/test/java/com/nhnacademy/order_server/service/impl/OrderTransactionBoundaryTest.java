package com.nhnacademy.order_server.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class OrderTransactionBoundaryTest {

    @Test
    @DisplayName("OrderServiceImpl은 클래스 전체 트랜잭션을 사용하지 않는다")
    void orderServiceImplDoesNotUseClassLevelTransaction() {
        assertThat(OrderServiceImpl.class.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    @DisplayName("주문 생성 오케스트레이션은 외부 I/O를 위해 트랜잭션 밖에서 시작한다")
    void createOrderIsNotTransactional() throws NoSuchMethodException {
        Method createOrder = OrderServiceImpl.class.getMethod(
                "createOrder",
                com.nhnacademy.order_server.dto.request.OrderCreateRequest.class
        );

        assertThat(createOrder.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    @DisplayName("결제 성공 후처리 오케스트레이션은 외부 I/O를 위해 트랜잭션 밖에서 실행된다")
    void processPaymentSuccessMessageIsNotTransactional() throws NoSuchMethodException {
        Method processPaymentSuccessMessage = OrderServiceImpl.class.getMethod(
                "processPaymentSuccessMessage",
                com.nhnacademy.order_server.dto.message.PaymentSuccessMessage.class
        );

        assertThat(processPaymentSuccessMessage.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    @DisplayName("주문 취소 오케스트레이션은 외부 보상 호출을 위해 트랜잭션 밖에서 실행된다")
    void cancelOrderOrchestrationIsNotTransactional() throws NoSuchMethodException {
        Method cancelOrderTransactional = OrderCancelService.class.getMethod(
                "cancelOrderTransactional",
                Long.class
        );

        assertThat(cancelOrderTransactional.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    @DisplayName("구매 확정 오케스트레이션은 포인트 메시지 발행을 위해 트랜잭션 밖에서 실행된다")
    void purchaseConfirmIsNotTransactional() throws NoSuchMethodException {
        Method purchaseConfirm = OrderServiceImpl.class.getMethod("purchaseConfirm", Long.class);

        assertThat(purchaseConfirm.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    @DisplayName("자동 구매확정과 만료 취소 배치는 외부 호출을 위해 트랜잭션 밖에서 실행된다")
    void schedulerBatchOrchestrationIsNotTransactional() throws NoSuchMethodException {
        Method autoConfirmPurchase = OrderServiceImpl.class.getMethod("autoConfirmPurchase");
        Method cancelExpiredOrders = OrderServiceImpl.class.getMethod("cancelExpiredOrders");

        assertThat(autoConfirmPurchase.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(cancelExpiredOrders.isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    @DisplayName("실제 주문 DB 저장 메서드가 트랜잭션 경계를 가진다")
    void createOrderPersistenceBoundaryIsTransactional() throws NoSuchMethodException {
        Method createOrderInTransaction = OrderCreateService.class.getMethod(
                "createOrderInTransaction",
                com.nhnacademy.order_server.dto.request.OrderCreateRequest.class,
                String.class,
                com.nhnacademy.order_server.dto.OrderCalculationData.class,
                com.nhnacademy.order_server.dto.request.OrderCreateRequest.OrderCalculationResult.class
        );

        assertThat(createOrderInTransaction.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    @DisplayName("주문 상태 변경 전담 메서드만 트랜잭션 경계를 가진다")
    void orderStatusMutationMethodsAreTransactional() throws NoSuchMethodException {
        Method markPaymentSuccess = OrderStatusMutationService.class.getMethod(
                "markPaymentSuccess",
                Long.class,
                String.class,
                Integer.class
        );
        Method markCanceled = OrderStatusMutationService.class.getMethod("markCanceled", Long.class);
        Method markPurchaseConfirmed = OrderStatusMutationService.class.getMethod("markPurchaseConfirmed", Long.class);

        assertThat(markPaymentSuccess.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(markCanceled.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(markPurchaseConfirmed.isAnnotationPresent(Transactional.class)).isTrue();
    }
}
