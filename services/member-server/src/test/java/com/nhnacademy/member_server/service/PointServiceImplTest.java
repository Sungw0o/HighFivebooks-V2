package com.nhnacademy.member_server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.nhnacademy.member_server.dto.request.point.PointAdminAdjustmentRequest;
import com.nhnacademy.member_server.dto.request.point.PointAdminPolicyRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.entity.member.Grade;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.entity.point.PointEventType;
import com.nhnacademy.member_server.entity.point.PointHistory;
import com.nhnacademy.member_server.entity.point.PointPolicy;
import com.nhnacademy.member_server.entity.point.PointStatus;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.repository.PointHistoryRepository;
import com.nhnacademy.member_server.repository.PointPolicyRepository;
import com.nhnacademy.member_server.service.impl.PointServiceImpl;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private PointPolicyRepository pointPolicyRepository;

    @InjectMocks
    private PointServiceImpl pointServiceImpl;

    // --- 1. 통합 트랜잭션 (createTransaction) 테스트 ---

    @Test
    @DisplayName("통합: 포인트 적립(EARN_ORDER) 성공")
    void createTransaction_EarnOrder_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 10000L);
        // 등급 적립률 1% 가정 (Member 생성 메서드 참고)
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_ORDER)
                .amount(50000L) // 50000 * 0.01 = 500원 적립 예상
                .orderId(100L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(10500L); // 10000 + 500

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        then(pointHistoryRepository).should().save(captor.capture());

        PointHistory saved = captor.getValue();
        assertThat(saved.getAmount()).isEqualTo(500L);
        assertThat(saved.getPointEventType()).isEqualTo(PointEventType.EARN_ORDER);
        assertThat(saved.getStatus()).isEqualTo(PointStatus.CONFIRMED);
    }

    @Test
    @DisplayName("통합: 포인트 사용(USE_ORDER) 성공")
    void createTransaction_UseOrder_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 10000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_ORDER)
                .amount(5000L)
                .orderId(200L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(5000L); // 10000 - 5000
        assertThat(member.getCurrentPoint()).isEqualTo(5000L);

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        then(pointHistoryRepository).should().save(captor.capture());

        // 사용은 음수로 저장됨
        assertThat(captor.getValue().getAmount()).isEqualTo(-5000L);
        assertThat(captor.getValue().getPointEventType()).isEqualTo(PointEventType.USE_ORDER);
    }

    @Test
    @DisplayName("통합: 사용 취소/환불(USE_CANCEL_ORDER) 성공")
    void createTransaction_Refund_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 5000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_CANCEL_ORDER)
                .amount(1000L)
                .orderId(200L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(6000L); // 5000 + 1000

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        then(pointHistoryRepository).should().save(captor.capture());

        assertThat(captor.getValue().getAmount()).isEqualTo(1000L);
        assertThat(captor.getValue().getPointEventType()).isEqualTo(PointEventType.USE_CANCEL_ORDER);
    }

    @Test
    @DisplayName("통합: 반품 시 적립금 회수(EARN_CANCEL_RETURN) 성공")
    void createTransaction_Deduct_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 1000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        // ★ [수정 2] 회수하려면 '원래 적립된 내역(EARN_ORDER)'이 DB에 있어야 함 (Mock 설정 추가)
        PointHistory originalEarn = PointHistory.builder()
                .member(member)
                .amount(500L) // 원래 500원 적립됐었다고 가정
                .orderId(300L)
                .pointEventType(PointEventType.EARN_ORDER)
                .build();
        ReflectionTestUtils.setField(originalEarn, "id", 123L); // ID도 필요하면 주입

        // EARN_ORDER 조회 시 originalEarn 리턴하도록 설정
        given(pointHistoryRepository.findByOrderIdAndPointEventType(300L, PointEventType.EARN_ORDER))
                .willReturn(Optional.of(originalEarn));

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_CANCEL_RETURN)
                .amount(500L)
                .orderId(300L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(500L); // 1000 - 500 = 500 (성공)

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        then(pointHistoryRepository).should().save(captor.capture());

        assertThat(captor.getValue().getAmount()).isEqualTo(-500L); // 차감 확인
        assertThat(captor.getValue().getPointEventType()).isEqualTo(PointEventType.EARN_CANCEL_RETURN);
    }

    @Test
    @DisplayName("통합: 정책 기반 적립(EARN_REVIEW) 성공")
    void createTransaction_EarnReview_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 10000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointPolicy policy = PointPolicy.builder().reviewPoint(200).build();
        given(pointPolicyRepository.findTopByOrderByUpdatedAtDesc()).willReturn(policy);

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_REVIEW)
                // amount 없음 -> 정책에서 가져옴
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(10200L);
    }

    @Test
    @DisplayName("통합: 환불 재적립(EARN_REFUND) - 중복 요청 시 무시")
    void createTransaction_EarnRefund_Duplicate_Ignore() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 10000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        // 이미 해당 주문건으로 환불 적립 내역이 존재함
        given(pointHistoryRepository.existsByOrderIdAndPointEventType(100L, PointEventType.EARN_REFUND))
                .willReturn(true);

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_REFUND)
                .amount(500L)
                .orderId(100L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(10000L); // 변화 없음
        then(pointHistoryRepository).should(never()).save(any(PointHistory.class));
    }


    // --- 2. TCC 관련 테스트 ---

    @Test
    @DisplayName("TCC 예약(Reserve) 성공 - 포인트 차감 및 상태 RESERVED")
    void tcc_Reserve_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 10000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));
        given(pointHistoryRepository.existsByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(false);

        // when
        pointServiceImpl.reservePoint(memberId, 5000L, 100L);

        // then
        assertThat(member.getCurrentPoint()).isEqualTo(5000L); // 10000 - 5000 (선차감)

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        then(pointHistoryRepository).should().save(captor.capture());

        PointHistory saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PointStatus.RESERVED);
        assertThat(saved.getPointEventType()).isEqualTo(PointEventType.USE_ORDER);
        assertThat(saved.getAmount()).isEqualTo(-5000L);
    }

    @Test
    @DisplayName("TCC 확정(Confirm) 성공")
    void tcc_Confirm_Success() {
        // given
        PointHistory history = PointHistory.builder()
                .orderId(100L)
                .status(PointStatus.RESERVED)
                .pointEventType(PointEventType.USE_ORDER)
                .build();

        given(pointHistoryRepository.findByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(Optional.of(history));

        // when
        pointServiceImpl.confirmPoint(1L, 5000L, 100L);

        // then
        assertThat(history.getStatus()).isEqualTo(PointStatus.CONFIRMED);
    }

    @Test
    @DisplayName("TCC 취소(Cancel) 성공 - 상태 취소 후 createTransaction으로 환불")
    void tcc_Cancel_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 5000L); // 이미 차감된 상태

        // 취소할 기존 예약 내역
        PointHistory history = PointHistory.builder()
                .orderId(100L)
                .status(PointStatus.RESERVED)
                .pointEventType(PointEventType.USE_ORDER)
                .build();

        ReflectionTestUtils.setField(history, "id", 999L);

        given(pointHistoryRepository.findByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(Optional.of(history));

        // createTransaction 내부에서 호출됨
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        // when
        pointServiceImpl.cancelPoint(memberId, 5000L, 100L);

        // then
        // 1. 기존 내역 상태 변경 확인
        assertThat(history.getStatus()).isEqualTo(PointStatus.CANCELED);

        // 2. 환불 트랜잭션(USE_CANCEL_ORDER)이 저장되었는지 확인
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        // TCC 취소 시: cancelPoint 내부에서 createTransaction 호출 -> save 발생
        // 주의: reserve 때 save 1번(Mock 아님 실제면), 여기서는 cancelPoint 메서드만 테스트하므로 save는 1번 호출됨
        then(pointHistoryRepository).should().save(captor.capture());

        PointHistory refundHistory = captor.getValue();
        assertThat(refundHistory.getPointEventType()).isEqualTo(PointEventType.USE_CANCEL_ORDER);
        assertThat(refundHistory.getAmount()).isEqualTo(5000L);
        assertThat(refundHistory.getOriginalPointHistoryId()).isEqualTo(999L);

        // 3. 멤버 잔액 복구 확인
        assertThat(member.getCurrentPoint()).isEqualTo(10000L);
    }

    // --- 3. 관리자 및 기타 테스트 ---

    @Test
    @DisplayName("관리자: 포인트 조정(지급) 성공")
    void admin_Adjustment_Earn_Success() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 0L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointAdminAdjustmentRequest request = new PointAdminAdjustmentRequest(memberId, 1000L, "이벤트 지급");

        // when
        pointServiceImpl.adjustmentMemberPoint(request);

        // then
        assertThat(member.getCurrentPoint()).isEqualTo(1000L);

        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        then(pointHistoryRepository).should().save(captor.capture());

        assertThat(captor.getValue().getPointEventType()).isEqualTo(PointEventType.EARN_ADMIN);
    }

    @Test
    @DisplayName("정책 수정 테스트")
    void updatePolicy_Success() {
        // given
        PointAdminPolicyRequest request = new PointAdminPolicyRequest(1000, 200, 500);

        // when
        pointServiceImpl.updatePolicy(request);

        // then
        then(pointPolicyRepository).should().save(any(PointPolicy.class));
    }

    @Test
    @DisplayName("예외: 적립 요청 시 주문번호나 금액이 누락되면 실패")
    void createTransaction_EarnOrder_InvalidInput() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 0L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        // 금액 누락
        PointTransactionCreateRequest requestNoAmount = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_ORDER)
                .orderId(100L)
                .amount(null)
                .build();

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.createTransaction(requestNoAmount))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("예외: 포인트 사용 시 잔액이 부족하면 실패")
    void createTransaction_UseOrder_NotEnoughPoint() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 1000L); // 잔액 1000원
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.USE_ORDER)
                .amount(5000L) // 5000원 사용 시도
                .orderId(200L)
                .build();

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.createTransaction(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_ENOUGH);
    }

    @Test
    @DisplayName("예외: 적립 정책(PointPolicy)이 DB에 없으면 실패")
    void createTransaction_PolicyNotFound() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 0L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        // 정책 조회 시 null 반환
        given(pointPolicyRepository.findTopByOrderByUpdatedAtDesc()).willReturn(null);

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_REVIEW) // 정책이 필요한 타입
                .build();

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.createTransaction(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_POLICY_NOT_FOUND);
    }

    @Test
    @DisplayName("분기: 적립 회수 시도했으나 이미 회수된 주문이면(중복 요청) 무시")
    void processEarnCancel_AlreadyDeducted_Ignore() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 5000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        // 1. 적립 내역은 존재함
        PointHistory originalEarn = PointHistory.builder().amount(100L).build();
        given(pointHistoryRepository.findByOrderIdAndPointEventType(300L, PointEventType.EARN_ORDER))
                .willReturn(Optional.of(originalEarn));

        // 2. ★ 이미 회수 내역(EARN_CANCEL_RETURN)이 존재함
        given(pointHistoryRepository.existsByOrderIdAndPointEventType(300L, PointEventType.EARN_CANCEL_RETURN))
                .willReturn(true);

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_CANCEL_RETURN)
                .orderId(300L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(5000L); // 잔액 변화 없음
        // save 호출 안됨
        then(pointHistoryRepository).should(never()).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("분기: 적립 회수 시도했으나 애초에 적립된 적 없는 주문이면 무시")
    void processEarnCancel_NoOriginalEarn_Ignore() {
        // given
        Long memberId = 1L;
        // 1. 적립 내역 없음 (Empty)
        given(pointHistoryRepository.findByOrderIdAndPointEventType(400L, PointEventType.EARN_ORDER))
                .willReturn(Optional.empty());

        // (Member 조회는 originalEarn 체크 후에 일어나는지, 전에 일어나는지에 따라 Mocking 필요 여부 결정됨)
        // 코드상: originalEarn 조회 -> Member 조회 순서이므로 Member 조회까지 Mocking 필요
        Member member = createMember(memberId, 5000L);
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointTransactionCreateRequest request = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .pointEventType(PointEventType.EARN_CANCEL_RETURN)
                .orderId(400L)
                .build();

        // when
        Long result = pointServiceImpl.createTransaction(request);

        // then
        assertThat(result).isEqualTo(5000L);
        then(pointHistoryRepository).should(never()).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("TCC 예약: 이미 예약된 주문번호면(중복 요청) 무시")
    void tcc_Reserve_Duplicate_Ignore() {
        // given
        given(pointHistoryRepository.existsByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(true); // 이미 존재

        // when
        pointServiceImpl.reservePoint(1L, 1000L, 100L);

        // then
        // save 호출 안됨 (deductPoint 내부 로직 실행 X)
        then(pointHistoryRepository).should(never()).save(any(PointHistory.class));
    }

    @Test
    @DisplayName("TCC 확정: 예약 내역이 없으면 실패")
    void tcc_Confirm_NotFound() {
        // given
        given(pointHistoryRepository.findByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.confirmPoint(1L, 1000L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_FOUND);
    }

    @Test
    @DisplayName("TCC 확정: 이미 확정된 상태면 무시(성공 처리)")
    void tcc_Confirm_AlreadyConfirmed() {
        // given
        PointHistory history = PointHistory.builder()
                .status(PointStatus.CONFIRMED) // 이미 확정됨
                .build();
        given(pointHistoryRepository.findByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(Optional.of(history));

        // when
        pointServiceImpl.confirmPoint(1L, 1000L, 100L);

        // then
        // 상태 변경 로직이 다시 돌지 않아야 함 (이미 CONFIRMED이므로)
        // (JPA Dirty Checking이라 명시적 save 검증은 어렵지만, 에러가 안 나는 것을 확인)
    }

    @Test
    @DisplayName("TCC 확정: 상태가 RESERVED가 아니면(예: CANCELED) 실패")
    void tcc_Confirm_InvalidStatus() {
        // given
        PointHistory history = PointHistory.builder()
                .status(PointStatus.CANCELED) // 취소된 건을 확정하려 함
                .build();
        given(pointHistoryRepository.findByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(Optional.of(history));

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.confirmPoint(1L, 1000L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("TCC 취소: 이미 취소된 상태면 무시")
    void tcc_Cancel_AlreadyCanceled() {
        // given
        PointHistory history = PointHistory.builder()
                .status(PointStatus.CANCELED) // 이미 취소됨
                .build();
        given(pointHistoryRepository.findByOrderIdAndPointEventType(100L, PointEventType.USE_ORDER))
                .willReturn(Optional.of(history));

        // when
        pointServiceImpl.cancelPoint(1L, 1000L, 100L);

        // then
        // createTransaction(환불) 호출되지 않음
        then(memberRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자: 차감 조정 시 잔액 부족이면 실패")
    void admin_Adjustment_Deduct_NotEnough() {
        // given
        Long memberId = 1L;
        Member member = createMember(memberId, 500L); // 잔액 500
        given(memberRepository.findByIdForUpdate(memberId)).willReturn(Optional.of(member));

        PointAdminAdjustmentRequest request = new PointAdminAdjustmentRequest(memberId, -1000L, "차감"); // 1000원 차감 시도

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.adjustmentMemberPoint(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_ENOUGH);
    }

    @Test
    @DisplayName("조회: 존재하지 않는 회원 이력 조회 시 실패")
    void getHistory_MemberNotFound() {
        // given
        given(memberRepository.existsById(999L)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> pointServiceImpl.getHistory(999L, Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    // --- Helper Method ---
    private Member createMember(Long id, Long point) {
        Grade grade = Grade.builder()
                .gradeName("GENERAL")
                .pointRate(BigDecimal.valueOf(0.01)) // 1%
                .build();

        return Member.builder()
                .id(id)
                .loginId("test" + id)
                .currentPoint(point)
                .grade(grade)
                .build();
    }
}