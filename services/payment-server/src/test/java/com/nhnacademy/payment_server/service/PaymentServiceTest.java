package com.nhnacademy.payment_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment_server.adaptor.TossPaymentAdapter;
import com.nhnacademy.payment_server.client.OrderClient;
import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.OrderValidationInfoResponse;
import com.nhnacademy.payment_server.dto.response.PaymentCancelResponse;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import com.nhnacademy.payment_server.service.impl.PaymentServiceImpl;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private TossPaymentAdapter tossPaymentAdapter;
    @Mock
    private OrderClient orderClient;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private PaymentOutboxService outboxService;



    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    @DisplayName("결제 승인 성공: 모든 검증 통과 및 저장 완료")
    void confirmPayment_Success() {
        String paymentKey = "test_key";
        String tossOrderId = "toss_123";
        Long orderId = 100L;
        Long amount = 50000L;

        PaymentConfirmRequest request = new PaymentConfirmRequest(paymentKey, tossOrderId, amount, "TOSS");

        OrderValidationInfoResponse orderDto = OrderValidationInfoResponse.builder()
                .orderId(orderId)
                .paymentAmount(amount)
                .build();
        given(orderClient.getOrderByKey(tossOrderId)).willReturn(orderDto);

        TossConfirmResponse tossResponse = TossConfirmResponse.builder()
                .method("카드")
                .status("DONE")
                .totalAmount(amount)
                .paymentKey(paymentKey)
                .requestedAt(OffsetDateTime.now())
                .approvedAt(OffsetDateTime.now())
                .build();

        given(tossPaymentAdapter.requestConfirm(any(), any(), any())).willReturn(tossResponse);

        PaymentMethod method = new PaymentMethod("TOSS", "토스", true);

        given(paymentMethodRepository.findByName("TOSS")).willReturn(Optional.of(method));

        Payment savedPayment = new Payment(method, orderId, LocalDateTime.now(), LocalDateTime.now(), PaymentStatus.DONE, paymentKey, amount);

        ReflectionTestUtils.setField(savedPayment, "id", 1L);

        given(outboxService.savePaymentAndOutbox(any(Payment.class), any(PaymentMessageOutbox.class)))
                .willReturn(savedPayment);

        PaymentConfirmResponse response = paymentService.confirmPayment(request);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.DONE);

        verify(outboxService).savePaymentAndOutbox(any(Payment.class), any(PaymentMessageOutbox.class));
    }

    @Test
    @DisplayName("결제 실패시 예외")
    void confirmPayment_Rollback(){
        // given
        PaymentConfirmRequest request = new PaymentConfirmRequest("testKey", "apple", 10000L, "TOSS");

        PaymentMethod mockMethod = PaymentMethod.builder().name("TOSS").isActive(true).build();
        given(paymentMethodRepository.findByName("TOSS")).willReturn(Optional.of(mockMethod));

        OrderValidationInfoResponse orderDto = OrderValidationInfoResponse.builder()
                .orderId(100L)
                .paymentAmount(10000L)
                .build();
        given(orderClient.getOrderByKey("apple")).willReturn(orderDto);

        given(tossPaymentAdapter.requestConfirm(any(), any(), any()))
                .willThrow(new RuntimeException("Toss 통신 오류"));

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOSS_API_ERROR);
    }

    @Test
    @DisplayName("결제 실패: 금액 불일치 (위변조 감지)")
    void confirmPayment_Fail_AmountMismatch() {
        String tossOrderId = "hack_order";
        PaymentConfirmRequest request = new PaymentConfirmRequest("key", tossOrderId, 100L, "TOSS"); // 100원 요청

        PaymentMethod mockMethod = PaymentMethod.builder().name("TOSS").isActive(true).build();
        given(paymentMethodRepository.findByName("TOSS")).willReturn(Optional.of(mockMethod));

        OrderValidationInfoResponse orderDto = OrderValidationInfoResponse.builder()
                .orderId(100L)
                .paymentAmount(10000L)
                .build();
        given(orderClient.getOrderByKey(tossOrderId)).willReturn(orderDto);

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("결제 취소 성공: 상태 변경 및 Toss 취소")
    void cancelPayment_Success() {
        // given
        String paymentKey = "test_cancel_key";
        PaymentCancelRequest request = new PaymentCancelRequest(paymentKey, "단순 변심");

        Payment payment = Payment.builder()
                .paymentMethod(new PaymentMethod("TOSS", "토스", true))
                .orderId(100L)
                .status(PaymentStatus.DONE) // 이미 완료된 상태
                .paymentKey(paymentKey)
                .amount(50000L)
                .build();
        ReflectionTestUtils.setField(payment, "id", 10L);

        given(paymentRepository.findByPaymentKeyForUpdate(anyString()))
                .willReturn(Optional.of(payment));

        // when
        PaymentCancelResponse response = paymentService.cancelPayment("key", request);

        // then
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);

        // Toss 취소 API 호출 확인
        verify(tossPaymentAdapter).requestCancel(anyString(), any());
    }

    @Test
    @DisplayName("결제 취소 실패: 이미 취소된 결제")
    void cancelPayment_Fail_AlreadyCanceled() {
        // given
        String paymentKey = "already_canceled_key";
        PaymentCancelRequest request = new PaymentCancelRequest(paymentKey, "중복 취소");

        Payment payment = Payment.builder()
                .status(PaymentStatus.CANCELED)
                .paymentKey(paymentKey)
                .build();

        given(paymentRepository.findByPaymentKeyForUpdate(paymentKey))
                .willReturn(Optional.of(payment));

        PaymentCancelResponse response = paymentService.cancelPayment(request.getPaymentKey(), request);

        assertThat(response.getCancelReason()).isEqualTo("중복 취소");

        // Toss 호출은 안 됨
        verify(tossPaymentAdapter, never()).requestCancel(any(), any());
    }


    @Test
    @DisplayName("결제 취소 실패: 존재하지 않는 결제 키")
    void cancelPayment_Fail_NotFound() {
        String paymentKey = "unknown_key";
        PaymentCancelRequest request = new PaymentCancelRequest(paymentKey, "사유");

        given(paymentRepository.findByPaymentKeyForUpdate(paymentKey)).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.cancelPayment(paymentKey, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 실패: 지원하지 않는 결제 수단")
    void confirmPayment_Fail_InvalidMethod() {
        PaymentConfirmRequest request = new PaymentConfirmRequest("key", "order", 100L, "KAKAO");

        given(paymentMethodRepository.findByName("KAKAO")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.METHOD_NOT_FOUND);
    }

    @Test
    @DisplayName("결제 실패: 지원 예정인 결제 수단")
    void confirmPayment_Fail_PointMethod() {
        PaymentConfirmRequest request = new PaymentConfirmRequest("key", "order", 100L, "BANK_TRANSFER");

        PaymentMethod mockMethod = PaymentMethod.builder().name("BANK_TRANSFER").isActive(true).build();
        given(paymentMethodRepository.findByName("BANK_TRANSFER")).willReturn(Optional.of(mockMethod));

        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_IMPLEMENTED);
    }
}
