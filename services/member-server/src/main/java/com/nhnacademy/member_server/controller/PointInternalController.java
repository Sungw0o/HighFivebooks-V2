package com.nhnacademy.member_server.controller;

import com.nhnacademy.member_server.controller.swagger.PointInternalApi;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionRequest;
import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointTransactionResponse;
import com.nhnacademy.member_server.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/point-transactions")
@Slf4j
public class PointInternalController implements PointInternalApi {

    private final PointService pointService;

    /**
     * [통합 API] 포인트 트랜잭션 생성
     * 적립(EARN), 사용(USE), 환불(CANCEL_USE), 회수(CANCEL_EARN) 모두 처리
     */
    @PostMapping
    public ResponseEntity<PointTransactionResponse> createTransaction(
            @RequestBody PointTransactionCreateRequest request) {

        Long currentPoint = pointService.createTransaction(request);

        PointTransactionResponse response = new PointTransactionResponse(request.getMemberId(), currentPoint);
        return ResponseEntity.ok(response);
    }

    /**
     * [TCC] 포인트 사용 예약 (Reserve)
     * 분산 트랜잭션 1단계: 포인트 차감 대기 상태
     */
    @PostMapping("/tcc/reserve")
    public ResponseEntity<Void> reservePoint(@RequestBody PointTransactionRequest request) {
        log.info("TCC Reserve 요청: {}", request);
        pointService.reservePoint(request.getMemberId(), request.getAmount(), request.getOrderId());
        return ResponseEntity.ok().build();
    }

    /**
     * [TCC] 포인트 사용 확정 (Confirm)
     * 분산 트랜잭션 2단계: 예약된 포인트 사용 확정
     */
    @PostMapping("/tcc/confirm")
    public ResponseEntity<Void> confirmPoint(@RequestBody PointTransactionRequest request) {
        log.info("TCC Confirm 요청: {}", request);
        pointService.confirmPoint(request.getMemberId(), request.getAmount(), request.getOrderId());
        return ResponseEntity.ok().build();
    }

    /**
     * [TCC] 포인트 사용 취소 (Cancel)
     * 분산 트랜잭션 보상: 예약된 포인트 취소 (환불)
     */
    @PostMapping("/tcc/cancel")
    public ResponseEntity<Void> cancelPoint(@RequestBody PointTransactionRequest request) {
        log.info("TCC Cancel 요청: {}", request);
        pointService.cancelPoint(request.getMemberId(), request.getAmount(), request.getOrderId());
        return ResponseEntity.ok().build();
    }

    /**
     * [조회] 회원 포인트 잔액 조회
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<PointBalanceResponse> getPointBalance(@PathVariable("memberId") Long memberId) {
        PointBalanceResponse balance = pointService.getBalance(memberId);
        return ResponseEntity.ok(balance);
    }
}