package com.nhnacademy.order_server.listener;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import com.nhnacademy.order_server.dto.message.PaymentSuccessMessage;
import com.nhnacademy.order_server.exception.OrderErrorCode;
import com.nhnacademy.order_server.exception.OrderException;
import com.nhnacademy.order_server.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentMessageListenerTest {

    @Mock
    private OrderService orderService;

    private PaymentMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new PaymentMessageListener(orderService);
    }

    @Test
    @DisplayName("결제 성공 메시지를 주문 후처리 서비스로 위임한다")
    void handlePaymentSuccessDelegatesToOrderService() {
        PaymentSuccessMessage message = PaymentSuccessMessage.builder()
                .orderId(1L)
                .paymentKey("payment-key")
                .totalAmount(30_000L)
                .build();

        listener.handlePaymentSuccess(message);

        then(orderService).should().processPaymentSuccessMessage(message);
    }

    @Test
    @DisplayName("금액 불일치 예외를 삼키지 않고 Rabbit retry/DLQ 경로로 전파한다")
    void amountMismatchExceptionIsPropagatedToRabbitRetryAndDlq() {
        PaymentSuccessMessage message = PaymentSuccessMessage.builder()
                .orderId(1L)
                .paymentKey("payment-key")
                .totalAmount(10_000L)
                .build();
        OrderException amountMismatch = new OrderException(OrderErrorCode.INVALID_REQUEST);
        doThrow(amountMismatch).when(orderService).processPaymentSuccessMessage(message);

        assertThatThrownBy(() -> listener.handlePaymentSuccess(message))
                .isSameAs(amountMismatch);

        then(orderService).should().processPaymentSuccessMessage(message);
    }
}
