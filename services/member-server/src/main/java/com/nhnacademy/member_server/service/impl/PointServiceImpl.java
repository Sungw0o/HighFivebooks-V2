package com.nhnacademy.member_server.service.impl;

import static com.nhnacademy.member_server.exception.ErrorCode.INVALID_INPUT_VALUE;
import static com.nhnacademy.member_server.exception.ErrorCode.MEMBER_NOT_FOUND;
import static com.nhnacademy.member_server.exception.ErrorCode.POINT_NOT_ENOUGH;
import static com.nhnacademy.member_server.exception.ErrorCode.POINT_NOT_ORDER_ID;
import static com.nhnacademy.member_server.exception.ErrorCode.POINT_POLICY_NOT_FOUND;

import com.nhnacademy.member_server.dto.request.point.PointAdminAdjustmentRequest;
import com.nhnacademy.member_server.dto.request.point.PointAdminPolicyRequest;
import com.nhnacademy.member_server.dto.request.point.PointTransactionCreateRequest;
import com.nhnacademy.member_server.dto.response.point.PointAdminPolicyResponse;
import com.nhnacademy.member_server.dto.response.point.PointBalanceResponse;
import com.nhnacademy.member_server.dto.response.point.PointHistoryResponse;
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
import com.nhnacademy.member_server.service.PointService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PointServiceImpl implements PointService {
    private final MemberRepository memberRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PointPolicyRepository pointPolicyRepository;

    /**
     * 통합 포인트 트랜잭션 생성 (적립, 사용, 환불/취소, 회수 통합)
     */
    @Override
    public Long createTransaction(PointTransactionCreateRequest request) {
        return switch (request.getPointEventType()) {
            case EARN_ORDER, EARN_REVIEW, EARN_PHOTO_REVIEW, EARN_SIGNUP, EARN_REFUND ->
                    processEarn(request);
            case USE_ORDER ->
                    processUse(request);
            case USE_CANCEL_ORDER, USE_CANCEL_RETURN ->
                    processRefund(request);
            case EARN_CANCEL_RETURN ->
                    processEarnCancel(request);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }

    // [내부 로직 1] 적립 처리
    private Long processEarn(PointTransactionCreateRequest request) {
        Member member = memberRepository.findByIdForUpdate(request.getMemberId())
                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        long pointToEarn;
        Long orderIdToSave = null;
        PointEventType eventType = request.getPointEventType();

        switch (eventType) {
            case EARN_ORDER -> {
                if (request.getOrderId() == null || request.getAmount() == null) {
                    throw new BusinessException(INVALID_INPUT_VALUE);
                }
                BigDecimal rate = member.getGrade().getPointRate();
                pointToEarn = BigDecimal.valueOf(request.getAmount()).multiply(rate).longValue();
                orderIdToSave = request.getOrderId();
            }
            case EARN_REFUND -> {
                if (request.getOrderId() == null) throw new BusinessException(INVALID_INPUT_VALUE);

                if (pointHistoryRepository.existsByOrderIdAndPointEventType(
                        request.getOrderId(), PointEventType.EARN_REFUND)) {
                    log.warn("이미 처리된 환불 적립 요청입니다.: orderId={}", request.getOrderId());
                    return member.getCurrentPoint();
                }
                pointToEarn = request.getAmount();
                orderIdToSave = request.getOrderId();
            }
            default -> {
                PointPolicy policy = pointPolicyRepository.findTopByOrderByUpdatedAtDesc();
                if (policy == null) throw new BusinessException(POINT_POLICY_NOT_FOUND);

                pointToEarn = switch (eventType) {
                    case EARN_REVIEW -> policy.getReviewPoint();
                    case EARN_PHOTO_REVIEW -> policy.getPhotoPoint();
                    case EARN_SIGNUP -> policy.getSignupPoint();
                    case EARN_REVIEW_UPGRADE -> Math.max(policy.getPhotoPoint() - policy.getReviewPoint(), 0L);
                    default -> 0L;
                };
            }
        }

        if (pointToEarn > 0) {
            long newPointBalance = member.getCurrentPoint() + pointToEarn;
            member.setCurrentPoint(newPointBalance);

            String description = (orderIdToSave != null)
                    ? formatDescription(eventType, orderIdToSave)
                    : eventType.getDescription();

            pointHistoryRepository.save(PointHistory.builder()
                    .orderId(orderIdToSave)
                    .member(member)
                    .amount(pointToEarn)
                    .description(description)
                    .pointEventType(eventType)
                    .pointBalance(newPointBalance)
                    .status(PointStatus.CONFIRMED)
                    .originalPointHistoryId(request.getOriginalPointHistoryId())
                    .build());

            return newPointBalance;
        }

        return member.getCurrentPoint();
    }

    // [내부 로직 2] 사용 처리
    private Long processUse(PointTransactionCreateRequest request) {
        // CONFIRMED 상태로 호출
        return deductPoint(
                request.getMemberId(),
                request.getAmount(),
                request.getOrderId(),
                request.getPointEventType(),
                PointStatus.CONFIRMED
        );
    }

    // [내부 로직 3] 환불/취소 처리
    private Long processRefund(PointTransactionCreateRequest request) {
        Member member = memberRepository.findByIdForUpdate(request.getMemberId())
                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        long amount = request.getAmount();
        long newBalance = member.getCurrentPoint() + amount;
        member.setCurrentPoint(newBalance);

        String description = formatDescription(request.getPointEventType(), request.getOrderId());

        pointHistoryRepository.save(PointHistory.builder()
                .member(member)
                .orderId(request.getOrderId())
                .amount(amount)
                .description(description)
                .pointEventType(request.getPointEventType())
                .description(request.getPointEventType().getDescription())
                .pointBalance(newBalance)
                .status(PointStatus.CONFIRMED)
                .originalPointHistoryId(request.getOriginalPointHistoryId())
                .build());

        return newBalance;
    }

    private Long processEarnCancel(PointTransactionCreateRequest request) {
        // 1. 해당 주문으로 '상품 구매 적립(EARN_ORDER)'된 내역이 있는지 조회
        PointHistory originalEarn = pointHistoryRepository.findByOrderIdAndPointEventType(
                        request.getOrderId(), PointEventType.EARN_ORDER).orElse(null);

        // 2. 적립 내역이 없으면(적립 안 된 주문이면) 회수할 것도 없으니 현재 잔액 리턴하고 종료
        Member member = memberRepository.findByIdForUpdate(request.getMemberId())
                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        if (originalEarn == null) {
            log.info("회수할 적립 내역이 존재하지 않습니다. orderId={}", request.getOrderId());
            return member.getCurrentPoint();
        }

        // 이미 회수된 적이 있는지 체크 (중복 회수 방지)
        boolean alreadyDeducted = pointHistoryRepository.existsByOrderIdAndPointEventType(
                request.getOrderId(), PointEventType.EARN_CANCEL_RETURN);
        if (alreadyDeducted) {
            log.warn("이미 적립 회수된 주문입니다. orderId={}", request.getOrderId());
            return member.getCurrentPoint();
        }

        // 3. 실제 적립되었던 금액만큼 차감 (request.getAmount() 무시)
        long amountToDeduct = originalEarn.getAmount();
        long newBalance = member.getCurrentPoint() - amountToDeduct;
        member.setCurrentPoint(newBalance);

        String description = "반품으로 인한 적립 포인트 회수 (주문번호: " + request.getOrderId() + ")";

        pointHistoryRepository.save(PointHistory.builder()
                .member(member)
                .orderId(request.getOrderId())
                .amount(-amountToDeduct) // 음수 저장
                .pointEventType(PointEventType.EARN_CANCEL_RETURN)
                .description(description)
                .pointBalance(newBalance)
                .status(PointStatus.CONFIRMED)
                .originalPointHistoryId(originalEarn.getId()) // 원본 내역 연결
                .build());

        return newBalance;
    }

    // --- 조회 및 관리자 메서드 ---
    @Override
    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));
        Long totalEarned = pointHistoryRepository.sumEarnedPoints(memberId);
        return new PointBalanceResponse(member.getId(), member.getCurrentPoint(), totalEarned != null ? totalEarned : 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointHistoryResponse> getHistory(Long memberId, Pageable pageable){
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(MEMBER_NOT_FOUND);
        }
        return pointHistoryRepository.findAllByMemberId(memberId, pageable).map(PointHistoryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public PointAdminPolicyResponse getRecentPolicy(){
        PointPolicy policy = pointPolicyRepository.findTopByOrderByUpdatedAtDesc();
        if(policy == null) throw new BusinessException(ErrorCode.POINT_POLICY_NOT_FOUND);
        return PointAdminPolicyResponse.from(policy);
    }

    @Override
    public void updatePolicy(PointAdminPolicyRequest requestDto){
        pointPolicyRepository.save(PointPolicy.builder()
                .signupPoint(requestDto.getSignupPoint())
                .reviewPoint(requestDto.getReviewPoint())
                .photoPoint(requestDto.getPhotoPoint())
                .build());
    }

    @Override
    public Long adjustmentMemberPoint(PointAdminAdjustmentRequest requestDto) {
        Member member = memberRepository.findByIdForUpdate(requestDto.getMemberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        long amount = requestDto.getAmount();
        if (amount == 0) throw new BusinessException(INVALID_INPUT_VALUE);

        PointEventType eventType;
        if (amount > 0) {
            eventType = PointEventType.EARN_ADMIN;
        } else {
            if (member.getCurrentPoint() < Math.abs(amount)) {
                throw new BusinessException(ErrorCode.POINT_NOT_ENOUGH);
            }
            eventType = PointEventType.USE_ADMIN;
        }

        long newBalance = member.getCurrentPoint() + amount;
        member.setCurrentPoint(newBalance);

        String description = String.format("%s (사유: %s)", eventType.getDescription(), requestDto.getReason());

        // 생성자 대신 builder 패턴으로 통일 (originalPointHistoryId는 자동 null 처리)
        pointHistoryRepository.save(PointHistory.builder()
                .member(member)
                .amount(amount)
                .description(description)
                .pointEventType(eventType)
                .pointBalance(newBalance)
                .status(PointStatus.CONFIRMED)
                .build());

        return newBalance;
    }

    // --- TCC (예약/확정/취소) 메서드 ---
    @Override
    public void reservePoint(Long memberId, Long amount, Long orderId) {
        log.info("TCC Reserve 요청: memberId={}, amount={}, orderId={}", memberId, amount, orderId);

        if (pointHistoryRepository.existsByOrderIdAndPointEventType(orderId, PointEventType.USE_ORDER)) {
            log.warn("이미 처리된 예약 요청입니다.: orderId={}", orderId);
            return;
        }

        deductPoint(memberId, amount, orderId, PointEventType.USE_ORDER, PointStatus.RESERVED);

        log.info("TCC Reserve(차감/예약) 완료: memberId={}, amount={}", memberId, amount);
    }

    @Override
    public void confirmPoint(Long memberId, Long amount, Long orderId) {
        PointHistory history = pointHistoryRepository.findByOrderIdAndPointEventType(orderId, PointEventType.USE_ORDER)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));

        if (history.getStatus() == PointStatus.CONFIRMED) {
            log.info("이미 확정된 주문입니다: orderId={}", orderId);
            return;
        }

        if (history.getStatus() != PointStatus.RESERVED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        history.updateStatus(PointStatus.CONFIRMED);
        log.info("TCC Confirm(확정) 완료: memberId={}, orderId={}", memberId, orderId);
    }

    @Override
    public void cancelPoint(Long memberId, Long amount, Long orderId) {
        log.info("TCC Cancel 요청: memberId={}, orderId={}", memberId, orderId);

        PointHistory history = pointHistoryRepository.findByOrderIdAndPointEventType(orderId, PointEventType.USE_ORDER)
                .orElseThrow(() -> {
                    log.warn("취소할 내역이 없습니다. orderId={}", orderId);
                    return new BusinessException(ErrorCode.POINT_NOT_FOUND);
                });

        if (history.getStatus() == PointStatus.CANCELED) {
            log.warn("이미 취소된 주문입니다: orderId={}", orderId);
            return;
        }

        history.updateStatus(PointStatus.CANCELED);

        // ★ 수정: revertPoint(삭제됨) 대신 createTransaction을 통해 통합 환불 처리
        PointTransactionCreateRequest refundRequest = PointTransactionCreateRequest.builder()
                .memberId(memberId)
                .amount(amount)
                .orderId(orderId)
                .pointEventType(PointEventType.USE_CANCEL_ORDER)
                .originalPointHistoryId(history.getId()) // 취소된 예약의 ID 연결
                .build();

        createTransaction(refundRequest);

        log.info("TCC Cancel(환불) 완료: memberId={}, orderId={}", memberId, orderId);
    }

    // 공통 포인트 차감 로직
    private Long deductPoint(Long memberId, Long amount, Long orderId, PointEventType eventType, PointStatus status) {
        if (orderId == null) throw new BusinessException(POINT_NOT_ORDER_ID);
        if (amount == null || amount <= 0) throw new BusinessException(INVALID_INPUT_VALUE);

        Member member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(MEMBER_NOT_FOUND));

        if (member.getCurrentPoint() < amount) {
            throw new BusinessException(POINT_NOT_ENOUGH);
        }

        long newPointBalance = member.getCurrentPoint() - amount;
        member.setCurrentPoint(newPointBalance);

        String description = formatDescription(eventType, orderId);

        pointHistoryRepository.save(PointHistory.builder()
                .orderId(orderId)
                .member(member)
                .amount(-amount) // 음수 저장
                .description(description)
                .pointEventType(eventType)
                .pointBalance(newPointBalance)
                .status(status) // 상태값을 파라미터로 받음 (KEY POINT)
                .build());

        return newPointBalance;
    }

    private String formatDescription(PointEventType type, Long orderId) {
        return String.format("%s (주문번호: %d)", type.getDescription(), orderId);
    }
}