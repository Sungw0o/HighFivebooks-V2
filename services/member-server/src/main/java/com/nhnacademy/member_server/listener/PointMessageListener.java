package com.nhnacademy.member_server.listener;

import com.nhnacademy.member_server.dto.request.point.PointEarnRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointMessageListener {
    private final PointService pointService;

    @RabbitListener(queues = "point-queue")
    public void receiveMessage(PointEarnRequest requestDto){
        try {
            log.info("RabbitMQ 메시지 수신: type=[{}], memberId=[{}], amount=[{}]",
                    requestDto.getEventType(), requestDto.getMemberId(), requestDto.getPureAmount());

            PointTransactionCreateRequest createRequest = PointTransactionCreateRequest.builder()
                    .memberId(requestDto.getMemberId())
                    .amount(requestDto.getPureAmount())
                    .orderId(requestDto.getOrderId())
                    .pointEventType(requestDto.getEventType())
                    .build();

            pointService.createTransaction(createRequest);

            log.info("포인트 적립 완료");
        } catch (Exception e) {
            log.error("포인트 적립 실패! 메시지 소비 및 수동 적립 필요 request={}, error={}", requestDto, e.getMessage(), e);
        }
    }
}
