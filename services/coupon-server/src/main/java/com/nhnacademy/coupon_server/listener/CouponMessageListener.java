package com.nhnacademy.coupon_server.listener;

import com.nhnacademy.coupon_server.config.RabbitMqConfig;
import com.nhnacademy.coupon_server.dto.message.CouponIssueMessage;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.service.MemberCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponMessageListener {
    private final MemberCouponService memberCouponService;

    @RabbitListener(queues = RabbitMqConfig.COUPON_WELCOME_QUEUE)
    public void receiveWelcomeCouponRequest(CouponIssueMessage message) {
        log.info("RabbitMQ 웰컴 쿠폰 발급 요청 수신 - UserId: {}", message.getMemberId());

        try {
            memberCouponService.issueWelcomeCoupon(message.getMemberId());
        } catch (Exception e) {
            log.error("웰컴 쿠폰 발급 실패 - UserId: {}, Error: {}", message.getMemberId(), e.getMessage(), e);
            if (e instanceof TransientDataAccessException || e instanceof AmqpException) {
                throw e;
            }
        }
    }

    @RabbitListener(queues = RabbitMqConfig.COUPON_ISSUE_QUEUE)
    public void receiveIssueCouponRequest(CouponIssueMessage message) {
        log.info("RabbitMQ 일반 쿠폰 발급 요청 수신 - UserId: {}, CouponId: {}", message.getMemberId(), message.getCouponId());
        try {
            memberCouponService.createMemberCoupon(message.getMemberId(), message.getCouponId());
        } catch (DuplicateCouponException | CouponNotFoundException e) {
            // 비즈니스 예외는 재시도해도 결과가 같으므로 즉시 DLQ로 보냄 (Requeue False)
            log.warn("쿠폰 발급 비즈니스 예외 발생 (DLQ 이동) - User: {}, Error: {}", message.getMemberId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e);
        } catch (Exception e) {
            // 그 외 시스템 예외(DB 연결 실패 등)는 기본 정책(Retry 후 DLQ 등)을 따르도록 던짐
            log.error("시스템 오류 발생 - User: {}, Error: {}", message.getMemberId(), e.getMessage());
            throw e;
        }
    }

}
