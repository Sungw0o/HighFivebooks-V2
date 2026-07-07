package com.nhnacademy.payment_server.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.payment_server.adaptor.TossPaymentAdapter;
import com.nhnacademy.payment_server.client.OrderClient;
import com.nhnacademy.payment_server.dto.message.PaymentSuccessMessage;
import com.nhnacademy.payment_server.dto.request.PaymentCancelRequest;
import com.nhnacademy.payment_server.dto.request.PaymentConfirmRequest;
import com.nhnacademy.payment_server.dto.response.OrderValidationInfoResponse;
import com.nhnacademy.payment_server.dto.response.PaymentCancelResponse;
import com.nhnacademy.payment_server.dto.response.PaymentConfirmResponse;
import com.nhnacademy.payment_server.dto.response.TossConfirmResponse;
import com.nhnacademy.payment_server.entity.MessageStatus;
import com.nhnacademy.payment_server.entity.Payment;
import com.nhnacademy.payment_server.entity.PaymentMessageOutbox;
import com.nhnacademy.payment_server.entity.PaymentMethod;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.exception.BusinessException;
import com.nhnacademy.payment_server.exception.ErrorCode;
import com.nhnacademy.payment_server.repository.PaymentMethodRepository;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import com.nhnacademy.payment_server.service.PaymentOutboxService;
import com.nhnacademy.payment_server.service.PaymentService;
import feign.FeignException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrderClient orderClient; // Feign
    private final TossPaymentAdapter tossAdapter;
    private final ObjectMapper objectMapper;
    private final PaymentOutboxService outboxService;

    @Override
    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest requestDto) {
        PaymentMethod paymentMethod = paymentMethodRepository.findByName(requestDto.getPaymentMethod())
                .orElseThrow(() -> new BusinessException(ErrorCode.METHOD_NOT_FOUND));

        if (!paymentMethod.isActive()) {
            throw new BusinessException(ErrorCode.METHOD_DISABLED);
        }

        String provider = requestDto.getPaymentMethod().toUpperCase();

        return switch (provider) {
            case "TOSS" -> processTossPayment(requestDto, paymentMethod);
            case "BANK_TRANSFER", "VIRTUAL_ACCOUNT" -> throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
            default -> throw new BusinessException(ErrorCode.UNSUPPORTED_METHOD);
        };
    }

    public PaymentConfirmResponse processTossPayment(PaymentConfirmRequest requestDto, PaymentMethod paymentMethod) {
        log.info("결제 승인 요청 진입 orderKey: {}, amount: {}", requestDto.getOrderKey(), requestDto.getAmount());

        // 검증을 위한 주문 서버 결제 정보
        String orderKey = requestDto.getOrderKey();
        OrderValidationInfoResponse orderDto;

        try {
            orderDto = orderClient.getOrderByKey(orderKey);
        } catch (FeignException.NotFound e) {
            log.error("주문 정보를 찾을 수 없음: {}", orderKey);
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        } catch (FeignException e) {
            log.error("주문 서버 통신 오류: {}", orderKey, e);
            throw new BusinessException(ErrorCode.ORDER_SERVER_ERROR);
        }

        Long orderId = orderDto.getOrderId();
        Long realAmount = orderDto.getPaymentAmount();

        if (!Objects.equals(requestDto.getAmount(), realAmount)) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        // 토스 - 최종 결제 승인
        TossConfirmResponse tossResponse;
        try {
            tossResponse = tossAdapter.requestConfirm(requestDto.getPaymentKey(), orderKey, realAmount);
        } catch (Exception tossEx) {
            log.error("Toss 최종 승인 실패. 롤백 진행. orderKey: {}", orderKey, tossEx);

            throw new BusinessException(ErrorCode.TOSS_API_ERROR);
        }

        // 결제 정보 DB 저장
        Payment payment = Payment.builder()
                .paymentMethod(paymentMethod)
                .orderId(orderId)
                .requestedAt(tossResponse.getRequestedAt().toLocalDateTime())
                .approvedAt(tossResponse.getApprovedAt()
                        .atZoneSameInstant(ZoneId.of("Asia/Seoul"))
                        .toLocalDateTime())
                .status(tossResponse.getStatusEnum())
                .paymentKey(tossResponse.getPaymentKey())
                .amount(tossResponse.getTotalAmount())
                .build();

        // 메세지를 outbox에 저장
        PaymentMessageOutbox outbox;

        try {
            PaymentSuccessMessage message = new PaymentSuccessMessage(
                    payment.getOrderId(),
                    payment.getPaymentKey(),
                    payment.getAmount(),
                    payment.getApprovedAt()
            );

            // 객체 -> JSON 문자열 변환. 이때 에러가 나면 save가 안되는데, 결제는 된 상태
            String jsonPayload = objectMapper.writeValueAsString(message);

            outbox = PaymentMessageOutbox.builder()
                    .paymentId(null)
                    .payload(jsonPayload)
                    .status(MessageStatus.READY)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("메시지 JSON 변환 오류 발생. 결제 취소 진행. orderId={}", orderId, e);

            compensateTossPayment(tossResponse.getPaymentKey(), "System Error (메세지 파싱 오류)");

            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        try {
            // DB 저장이 필요한 순간에만 서비스 호출 (짧은 트랜잭션)
            Payment savedPayment = outboxService.savePaymentAndOutbox(payment, outbox);

            log.info("결제 및 Outbox DB 저장 완료. paymentId={}", savedPayment.getId());

            return PaymentConfirmResponse.from(savedPayment);

        } catch (Exception e) {
            log.error("DB 저장 실패! 승인된 Toss 결제를 자동 취소합니다. paymentKey={}", tossResponse.getPaymentKey(), e);

            compensateTossPayment(tossResponse.getPaymentKey(), "System Error (DB 저장 실패)");

            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public PaymentCancelResponse cancelPayment(String paymentKey, PaymentCancelRequest requestDto) {
        Payment payment = paymentRepository.findByPaymentKeyForUpdate(paymentKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getStatus() == PaymentStatus.CANCELED) {
            return PaymentCancelResponse.from(payment, requestDto.getCancelReason());
        }

        if (!payment.getStatus().equals(PaymentStatus.DONE)) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        try {
            tossAdapter.requestCancel(paymentKey, requestDto.getCancelReason());
        } catch (Exception e) {
            log.error("결제 취소 실패. paymentKey={}", paymentKey, e);

            throw new BusinessException(ErrorCode.TOSS_API_ERROR);
        }

        // DB 결제 상태 변경 (더티 체킹)
        payment.setStatus(PaymentStatus.CANCELED);
        payment.setCancelledAt(LocalDateTime.now());

        return PaymentCancelResponse.from(payment, requestDto.getCancelReason());
    }

    // 시스템 내부 오류 복구 목적의 보상 트랜잭션
    private void compensateTossPayment(String paymentKey, String reason) {
        try {
            tossAdapter.requestCancel(paymentKey, reason);
            log.info("결제 자동 취소 성공. paymentKey={}", paymentKey);
        } catch (Exception e) {
            log.error("CRITICAL: 결제 취소 실패. (관리자 수동 환불 필요) paymentKey={}", paymentKey, e);
        }
    }
}