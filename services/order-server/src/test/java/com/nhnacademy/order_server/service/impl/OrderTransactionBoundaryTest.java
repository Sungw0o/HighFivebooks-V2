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
}
