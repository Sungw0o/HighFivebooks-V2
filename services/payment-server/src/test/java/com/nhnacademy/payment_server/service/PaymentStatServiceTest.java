package com.nhnacademy.payment_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nhnacademy.payment_server.dto.response.DailySalesResponse;
import com.nhnacademy.payment_server.dto.response.PaymentStatsResponse;
import com.nhnacademy.payment_server.entity.PaymentStatus;
import com.nhnacademy.payment_server.repository.PaymentRepository;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentStatServiceTest {

    @InjectMocks
    private PaymentStatService paymentStatService;

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("전체 통계 조회: 데이터가 있을 경우 계산 로직 검증")
    void getTotalStats_Success() {
        // given
        Long salesAmount = 100_000L;
        Long cancelAmount = 20_000L;
        long successCount = 10L;
        long cancelCount = 2L;

        given(paymentRepository.sumAmountByStatusIn(List.of(PaymentStatus.DONE, PaymentStatus.CANCELED)))
                .willReturn(Optional.of(100_000L));
        given(paymentRepository.sumAmountByStatus(PaymentStatus.CANCELED))
                .willReturn(Optional.of(20_000L));
        given(paymentRepository.countByStatus(PaymentStatus.DONE))
                .willReturn(10L);
        given(paymentRepository.countByStatus(PaymentStatus.CANCELED))
                .willReturn(2L);


        // when
        PaymentStatsResponse response = paymentStatService.getTotalStats();

        // then
        assertThat(response.getTotalSalesAmount()).isEqualTo(salesAmount);
        assertThat(response.getTotalCancelAmount()).isEqualTo(cancelAmount);

        // 순매출 = 총매출 - 환불액
        assertThat(response.getNetSalesAmount()).isEqualTo(salesAmount - cancelAmount); // 80,000

        assertThat(response.getSuccessCount()).isEqualTo(successCount);
        assertThat(response.getCancelCount()).isEqualTo(cancelCount);

        // 총 트랜잭션 수 = 성공 + 취소
        assertThat(response.getTotalTransactionCount()).isEqualTo(successCount + cancelCount); // 12
    }

    @Test
    @DisplayName("전체 통계 조회: 데이터가 없을 경우 (0 반환 확인)")
    void getTotalStats_NoData() {
        given(paymentRepository.sumAmountByStatusIn(List.of(PaymentStatus.DONE, PaymentStatus.CANCELED)))
                .willReturn(Optional.empty());
        given(paymentRepository.sumAmountByStatus(PaymentStatus.CANCELED))
                .willReturn(Optional.empty());
        given(paymentRepository.countByStatus(PaymentStatus.DONE))
                .willReturn(0L);
        given(paymentRepository.countByStatus(PaymentStatus.CANCELED))
                .willReturn(0L);

        PaymentStatsResponse response = paymentStatService.getTotalStats();

        assertThat(response.getTotalSalesAmount()).isZero();
        assertThat(response.getTotalCancelAmount()).isZero();
        assertThat(response.getNetSalesAmount()).isZero();
        assertThat(response.getTotalTransactionCount()).isZero();
    }

    @Test
    @DisplayName("일별 매출 통계 조회: Repository 호출 확인")
    void getDailyStats_Success() {
        // given
        LocalDateTime start = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 1, 7, 23, 59);

        // DailySalesResponse는 Interface Projection이나 DTO일 수 있으므로 Mock List 반환으로 대체
        List<DailySalesResponse> mockResponse = Collections.emptyList();

        given(paymentRepository.findDailySalesBetween(start, end))
                .willReturn(mockResponse);

        // when
        List<DailySalesResponse> result = paymentStatService.getDailyStats(start, end);

        // then
        assertThat(result).isSameAs(mockResponse);
        verify(paymentRepository, times(1)).findDailySalesBetween(start, end);
    }
}