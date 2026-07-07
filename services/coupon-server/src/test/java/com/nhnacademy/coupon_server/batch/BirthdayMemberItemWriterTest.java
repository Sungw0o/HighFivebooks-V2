package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.impl.MemberCouponJdbcRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayMemberItemWriterTest {

    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private MemberCouponJdbcRepository memberCouponJdbcRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponDateCalculator dateCalculator;

    @InjectMocks
    private BirthdayMemberItemWriter writer;

    @Test
    @DisplayName("Writer 실행 시 쿠폰 정책을 한 번만 조회하고(캐싱), 청크 내 모든 회원에게 Bulk Insert로 쿠폰을 발급해야 한다")
    void writeSuccessWithCaching() {
        Chunk<Long> chunk = new Chunk<>(List.of(100L, 101L, 102L));
        Coupon birthdayCoupon = Coupon.builder().id(999L).build();

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of(birthdayCoupon));

        when(memberCouponRepository.findUserIdsByCouponIdAndUserIdIn(any(), any()))
                .thenReturn(List.of());

        when(dateCalculator.calculateExpiration(any())).thenReturn(LocalDateTime.now().plusDays(30));

        writer.beforeStep(mock(org.springframework.batch.core.StepExecution.class)); // 캐시 초기화
        writer.write(chunk);

        verify(couponRepository, times(1)).findCouponsByCommentAndStatus(any(), any(), any());

        verify(memberCouponJdbcRepository, times(1)).batchInsertMemberCoupons(argThat(list -> {
            // 저장하려는 리스트에 3명의 정보가 모두 들어있는지 검증
            boolean sizeMatch = list.size() == 3;
            boolean allCorrectUser = list.stream()
                    .map(mc -> mc.getUserId())
                    .allMatch(id -> List.of(100L, 101L, 102L).contains(id));

            return sizeMatch && allCorrectUser;
        }));
    }

    @Test
    @DisplayName("BeforeStep 실행 시 캐시 데이터(couponId)가 초기화되어, 재실행 시 다시 조회해야 한다")
    void beforeStep_ResetsCache() {
        Chunk<Long> chunk = new Chunk<>(List.of(1L));
        Coupon coupon = Coupon.builder().id(123L).build();

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of(coupon));

        // -------------------------------------------------------
        // 1. 첫 번째 실행 (First Execution)
        // -------------------------------------------------------
        writer.beforeStep(mock(org.springframework.batch.core.StepExecution.class));

        writer.write(chunk);

        verify(couponRepository, times(1)).findCouponsByCommentAndStatus(any(), any(), any());


        // -------------------------------------------------------
        // 2. 두 번째 실행 (Second Execution - 재시도 또는 다음 스텝 시뮬레이션)
        // -------------------------------------------------------
        writer.beforeStep(mock(org.springframework.batch.core.StepExecution.class));

        writer.write(chunk);

        verify(couponRepository, times(2)).findCouponsByCommentAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("활성화된 생일 쿠폰 정책을 찾을 수 없으면 IllegalStateException 발생")
    void beforeStep_ThrowsException_WhenNoPolicyFound() { // 메서드명 변경 권장 (write -> beforeStep)

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of()); // 빈 리스트 반환

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                writer.beforeStep(null)
        );
    }

    @Test
    @DisplayName("모든 유저가 이미 쿠폰을 발급받은 경우, Bulk Insert를 수행하지 않고 건너뛰어야 한다")
    void write_Skips_WhenAllUsersAlreadyIssued() {
        // Given
        Chunk<Long> chunk = new Chunk<>(List.of(100L, 101L, 102L));
        Coupon birthdayCoupon = Coupon.builder().id(999L).build();

        // 1. beforeStep: 쿠폰 정책 조회 Mocking
        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any()))
                .thenReturn(List.of(birthdayCoupon));

        // 2. write: 이미 발급된 유저 조회 Mocking (청크의 모든 유저가 이미 발급받았다고 가정)
        when(memberCouponRepository.findUserIdsByCouponIdAndUserIdIn(any(), any()))
                .thenReturn(List.of(100L, 101L, 102L));

        // beforeStep 실행하여 cachedBirthdayCoupon 설정
        writer.beforeStep(mock(org.springframework.batch.core.StepExecution.class));

        // When
        writer.write(chunk);

        // Then
        // 1. 이미 발급된 유저 조회가 수행되었는지 확인
        verify(memberCouponRepository, times(1)).findUserIdsByCouponIdAndUserIdIn(eq(999L), any());

        // 2. 핵심 검증: 타겟 유저가 없으므로 DB Insert가 절대 호출되지 않아야 함 (never)
        verify(memberCouponJdbcRepository, never()).batchInsertMemberCoupons(any());

        // 3. 만료일 계산 로직도 수행되지 않아야 함 (Skip 로직 이후에 위치하므로)
        verify(dateCalculator, never()).calculateExpiration(any());
    }
}