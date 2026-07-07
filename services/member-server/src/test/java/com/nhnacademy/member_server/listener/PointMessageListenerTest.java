package com.nhnacademy.member_server.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.dto.request.point.PointEarnRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.entity.point.PointEventType;
import com.nhnacademy.member_server.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointMessageListenerTest {

    @Mock
    private PointService pointService;

    @InjectMocks
    private PointMessageListener pointMessageListener;

    @Test
    @DisplayName("메시지 수신 성공 -> createTransaction 호출 검증")
    void receiveMessage_Success() {
        // given
        // 큐에서 들어온 메시지 객체
        PointEarnRequest message = new PointEarnRequest(1L, PointEventType.EARN_ORDER, 5000L, 100L);

        // when
        pointMessageListener.receiveMessage(message);

        // then
        // 1. Service의 통합 메서드(createTransaction)가 호출되었는지 검증
        ArgumentCaptor<PointTransactionCreateRequest> captor = ArgumentCaptor.forClass(PointTransactionCreateRequest.class);
        verify(pointService, times(1)).createTransaction(captor.capture());

        // 2. 리스너 내부에서 파라미터 매핑이 잘 되었는지 확인
        PointTransactionCreateRequest request = captor.getValue();
        assertThat(request.getMemberId()).isEqualTo(1L);
        assertThat(request.getPointEventType()).isEqualTo(PointEventType.EARN_ORDER);
        assertThat(request.getAmount()).isEqualTo(5000L);
        assertThat(request.getOrderId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("메시지 수신 실패: 서비스 에러 발생 시 예외 로그 처리 (테스트는 통과)")
    void receiveMessage_Exception() {
        // given
        PointEarnRequest message = new PointEarnRequest(1L, PointEventType.EARN_ORDER, 5000L, 100L);

        // createTransaction 호출 시 예외 발생 Stubbing
        doThrow(new RuntimeException("DB 연결 오류"))
                .when(pointService).createTransaction(any(PointTransactionCreateRequest.class));

        // when
        pointMessageListener.receiveMessage(message);

        // then
        // 예외가 발생해도 리스너 로직상 catch해서 처리했다면 호출 횟수는 1회여야 함
        verify(pointService, times(1)).createTransaction(any(PointTransactionCreateRequest.class));
    }
}