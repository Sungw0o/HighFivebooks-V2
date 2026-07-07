package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.calculator.CouponDateCalculator;
import com.nhnacademy.coupon_server.dto.request.CouponCalculationRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponCancelRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponIssueRequestDto;
import com.nhnacademy.coupon_server.dto.request.MemberCouponUseRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCalculationResponseDto;
import com.nhnacademy.coupon_server.dto.response.ErrorResponse;
import com.nhnacademy.coupon_server.dto.response.MemberCouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.CouponPolicyBook;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponNotFoundException;
import com.nhnacademy.coupon_server.exception.DuplicateCouponException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
import com.nhnacademy.coupon_server.exception.GlobalExceptionHandler;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.impl.MemberCouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberCouponServiceTest {
    @Mock
    private MemberCouponRepository memberCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponDateCalculator dateCalculator;

    @Mock
    private StringRedisTemplate redisTemplate;

    private MemberCouponServiceImpl memberCouponService;
    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @BeforeEach
    public void setUp() {

        memberCouponService = new MemberCouponServiceImpl(memberCouponRepository, couponRepository, dateCalculator, redisTemplate);
    }

    // ==========================================
    // 1. 관리자 발급 (issueCouponByAdmin)
    // ==========================================
    @Test
    @DisplayName("관리자 발급 성공")
    void issueCouponByAdmin_Success() {
        Long userId = 100L;
        Long couponId = 10L;
        LocalDateTime expectedDate = LocalDateTime.now().plusDays(30);

        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder()
                .userId(userId)
                .couponId(couponId)
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(expectedDate);

        memberCouponService.issueCouponByAdmin(requestDto);

        ArgumentCaptor<MemberCoupon> captor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponRepository).save(captor.capture());

        MemberCoupon savedCoupon = captor.getValue();
        Assertions.assertEquals(userId, savedCoupon.getUserId());
        Assertions.assertEquals(expectedDate, savedCoupon.getExpiredAt());
    }

    @Test
    @DisplayName("관리자 발급 실패 - 이미 발급된 쿠폰 (Logical Check)")
    void issueCouponByAdmin_Failure_Duplicate() {
        Long userId = 100L;
        Long couponId = 10L;
        MemberCouponIssueRequestDto requestDto = MemberCouponIssueRequestDto.builder().userId(userId).couponId(couponId).build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);

        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByAdmin(requestDto)
        );
        verify(memberCouponRepository, never()).save(any());
    }

    // ==========================================
    // 2. 사용자 발급 (issueCouponByUser)
    // [변경] Redis/RabbitMQ 제거 -> DB 직접 저장 검증
    // ==========================================
    @Test
    @DisplayName("사용자 발급 성공 - Lua Script 실행 결과 1 반환")
    void issueCouponByUser_Success() {
        Long userId = 1L;
        Long couponId = 100L;
        LocalDateTime expectedDate = LocalDateTime.now().plusDays(7);

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(expectedDate);

        // [핵심 변경] Redis Lua Script 실행 Mocking
        // execute(script, keys, args) 호출 시 1L(성공)을 반환하도록 설정
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(1L);

        memberCouponService.issueCouponByUser(userId, couponId);

        // DB 저장이 호출되었는지 검증
        verify(memberCouponRepository, times(1)).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("사용자 발급 성공 - Redis 불일치 복구 (Redis 중복 -1 리턴이나 DB에 없음)")
    void issueCouponByUser_RedisInconsistency_ProceedsToSave() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(-1L);

        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);

        when(dateCalculator.calculateExpiration(coupon)).thenReturn(LocalDateTime.now().plusDays(1));

        memberCouponService.issueCouponByUser(userId, couponId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 이미 발급된 유저 (Redis Lua Script 반환값 -1)")
    void issueCouponByUser_Failure_Duplicate_Redis() {
        Long userId = 1L;
        Long couponId = 100L;

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenReturn(-1L);

        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId))
                .thenReturn(true);

        Assertions.assertThrows(DuplicateCouponException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );

        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자 발급 실패 - 기간 만료 (Expired)")
    void issueCouponByUser_Fail_Expired() {
        Long couponId = 10L;
        Coupon coupon = Coupon.builder()
                .issuedStartAt(LocalDateTime.now().minusDays(10))
                .issuedEndAt(LocalDateTime.now().minusDays(1)) // 이미 종료됨
                .build();
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(IllegalArgumentException.class, () -> memberCouponService.issueCouponByUser(1L, couponId));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 정책 비활성화")
    void issueCouponByUser_Fail_InactivePolicy() {
        Long couponId = 10L;
        Coupon coupon = Coupon.builder()
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.INACTIVE).build())
                .build();
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        Assertions.assertThrows(IllegalStateException.class, () -> memberCouponService.issueCouponByUser(1L, couponId));
    }

    @Test
    @DisplayName("사용자 발급 실패 - 수량 매진 (Redis 0)")
    void issueCouponByUser_Fail_SoldOut() {
        Long userId = 1L;
        Long couponId = 10L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString())).thenReturn(0L);

        Assertions.assertThrows(IllegalStateException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    @Test
    @DisplayName("사용자 발급 실패 - Redis 연결 장애 발생")
    void issueCouponByUser_Fail_RedisError() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis Connection Failed"));

        Assertions.assertThrows(RedisConnectionFailureException.class, () ->
                memberCouponService.issueCouponByUser(userId, couponId)
        );
    }

    // ==========================================
    // 3. 웰컴 쿠폰 및 생일 쿠폰
    // ==========================================
    @Test
    @DisplayName("웰컴 쿠폰 발급 성공")
    void issueWelcomeCoupon_Success() {
        Long userId = 1L;
        Coupon welcomeCoupon = Coupon.builder().id(100L).couponName("Welcome").build();

        when(couponRepository.findCouponsByCommentAndStatus(eq(Comment.WELCOME), eq(CouponPolicyStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(List.of(welcomeCoupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, 100L)).thenReturn(false);
        when(dateCalculator.calculateExpiration(welcomeCoupon)).thenReturn(LocalDateTime.now().plusDays(30));

        memberCouponService.issueWelcomeCoupon(userId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 중단 - 이미 발급받음")
    void issueWelcomeCoupon_Skip_AlreadyExists() {
        Long userId = 1L;
        Coupon welcomeCoupon = Coupon.builder().id(100L).build();

        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any())).thenReturn(List.of(welcomeCoupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, 100L)).thenReturn(true);

        memberCouponService.issueWelcomeCoupon(userId);

        verify(memberCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("웰컴 쿠폰 발급 실패 - 정책 없음")
    void issueWelcomeCoupon_Fail_NoPolicy() {
        when(couponRepository.findCouponsByCommentAndStatus(any(), any(), any())).thenReturn(List.of());

        Assertions.assertThrows(CouponNotFoundException.class, () -> memberCouponService.issueWelcomeCoupon(1L));
    }

    @Test
    @DisplayName("생일 쿠폰 발급 성공")
    void issueBirthdayCoupon_Success() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(LocalDateTime.now().plusDays(30));

        memberCouponService.issueBirthdayCoupon(userId, couponId);

        verify(memberCouponRepository).save(any());
    }

    @Test
    @DisplayName("생일 쿠폰 발급 중단 - 이미 발급받음")
    void issueBirthdayCoupon_Skip_Duplicate() {
        Long userId = 1L;
        Long couponId = 100L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(true);

        memberCouponService.issueBirthdayCoupon(userId, couponId);

        verify(memberCouponRepository, never()).save(any());
    }

    // ==========================================
    // 4. 조회, 계산, 사용, 취소
    // ==========================================
    @Test
    @DisplayName("내부 생성용 (createMemberCoupon) 성공")
    void createMemberCoupon_Success() {
        Long userId = 1L;
        Long couponId = 50L;
        Coupon coupon = Coupon.builder().id(couponId).build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));
        when(memberCouponRepository.existsByUserIdAndCouponId(userId, couponId)).thenReturn(false);
        when(dateCalculator.calculateExpiration(coupon)).thenReturn(LocalDateTime.now().plusDays(10));

        memberCouponService.createMemberCoupon(userId, couponId);

        verify(memberCouponRepository).save(any(MemberCoupon.class));
    }

    @Test
    @DisplayName("회원 쿠폰 전체 조회 성공")
    void findAll_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Coupon coupon = Coupon.builder().couponPolicy(CouponPolicy.builder().discountType(DiscountType.FIXED).build()).build();
        Page<MemberCoupon> page = new PageImpl<>(List.of(MemberCoupon.builder().coupon(coupon).build()));

        when(memberCouponRepository.findAll(pageable)).thenReturn(page);

        Page<MemberCouponResponseDto> result = memberCouponService.findAll(pageable);
        Assertions.assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("특정 회원의 쿠폰 목록 조회")
    void findCouponByUserId_Success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        MemberCoupon mc = MemberCoupon.builder().coupon(Coupon.builder().couponPolicy(CouponPolicy.builder().discountType(DiscountType.FIXED).build()).build()).build();

        when(memberCouponRepository.findMemberCouponsByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(mc)));

        Page<MemberCouponResponseDto> result = memberCouponService.findCouponByUserId(userId, pageable);
        Assertions.assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("주문 시 사용 가능 쿠폰 조회 (필터링 로직 검증)")
    void findUsableCoupons_Filtering() {
        Long userId = 1L;
        List<Long> bookIds = List.of(100L);
        List<Long> categoryIds = List.of(10L);

        // 1. 일반 쿠폰 (모두 적용)
        Coupon generalCoupon = Coupon.builder()
                .id(1L)
                .couponName("General")
                .couponPolicy(CouponPolicy.builder()
                        .discountType(DiscountType.FIXED)
                        .discountValue(1000L)
                        .build())
                .build();
        MemberCoupon mc1 = MemberCoupon.builder().coupon(generalCoupon).build();

        // 2. 도서 전용 쿠폰 (책 ID: 100 - 매칭)
        CouponPolicy bookPolicy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();
        bookPolicy.getUsableBooks().add(CouponPolicyBook.builder().bookId(100L).build());

        Coupon bookCoupon = Coupon.builder()
                .id(2L)
                .couponName("Book Target")
                .couponPolicy(bookPolicy)
                .build();
        MemberCoupon mc2 = MemberCoupon.builder().coupon(bookCoupon).build();

        // 3. 다른 도서 전용 쿠폰 (책 ID: 999 - 불일치)
        CouponPolicy otherBookPolicy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();
        otherBookPolicy.getUsableBooks().add(CouponPolicyBook.builder().bookId(999L).build());

        Coupon otherBookCoupon = Coupon.builder()
                .id(3L)
                .couponName("Other Book")
                .couponPolicy(otherBookPolicy)
                .build();
        MemberCoupon mc3 = MemberCoupon.builder().coupon(otherBookCoupon).build();

        when(memberCouponRepository.findUsableCoupons(eq(userId), any(LocalDateTime.class)))
                .thenReturn(List.of(mc1, mc2, mc3));

        List<MemberCouponResponseDto> result = memberCouponService.findUsableCoupons(userId, bookIds, categoryIds);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(dto -> dto.getCouponName().equals("General")));
        Assertions.assertTrue(result.stream().anyMatch(dto -> dto.getCouponName().equals("Book Target")));
        Assertions.assertFalse(result.stream().anyMatch(dto -> dto.getCouponName().equals("Other Book")));
    }

    @Test
    @DisplayName("주문 시 사용 가능 쿠폰 조회 - bookIds가 null이거나 비어있으면 전체 반환")
    void findUsableCoupons_ReturnAllIfNoBooks() {
        MemberCoupon mc = MemberCoupon.builder()
                .coupon(Coupon.builder()
                        .couponPolicy(CouponPolicy.builder()
                                .discountType(DiscountType.FIXED)
                                .discountValue(1000L)
                                .build())
                        .build())
                .build();

        when(memberCouponRepository.findUsableCoupons(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(mc));

        List<MemberCouponResponseDto> result = memberCouponService.findUsableCoupons(1L, null, null);

        Assertions.assertEquals(1, result.size());
    }

    @Test
    @DisplayName("할인 계산 성공 - 금액 초과 시 주문금액까지만 할인")
    void calculateDiscount_CapAtOrderPrice() {
        Long memberCouponId = 55L;
        Long orderPrice = 3000L;

        CouponPolicy policy = CouponPolicy.builder()
                .discountType(DiscountType.FIXED).discountValue(5000L).minOrderValue(1000L).build();
        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId).userId(1L).coupon(Coupon.builder().couponPolicy(policy).build())
                .status(Status.ISSUED).expiredAt(LocalDateTime.now().plusDays(1)).build();

        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        CouponCalculationResponseDto res = memberCouponService.calculateDiscount(1L, new CouponCalculationRequestDto(memberCouponId, orderPrice));

        Assertions.assertEquals(3000L, res.getDiscountAmount());
        Assertions.assertEquals(0L, res.getFinalPrice());
    }

    @Test
    @DisplayName("쿠폰 사용 실패 - 소유자 불일치")
    void useCoupon_Fail_OwnerMismatch() {
        Long memberCouponId = 1L;
        MemberCoupon mc = MemberCoupon.builder().id(memberCouponId).userId(2L).build(); // 다른 유저
        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        MemberCouponUseRequestDto requestDto = new MemberCouponUseRequestDto(memberCouponId, 100L);

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                memberCouponService.useCoupon(1L, requestDto));
    }

    @Test
    @DisplayName("쿠폰 사용 성공")
    void useCoupon_Success() {
        Long userId = 1L;
        Long memberCouponId = 55L;

        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId)
                .userId(userId)
                .status(Status.ISSUED)
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();

        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        memberCouponService.useCoupon(userId, new MemberCouponUseRequestDto(memberCouponId, 123L));

        Assertions.assertEquals(Status.USED, mc.getStatus());
        Assertions.assertEquals(123L, mc.getOrderId());
    }

    @Test
    @DisplayName("쿠폰 취소 성공")
    void cancelCouponUsage_Success() {
        Long userId = 1L;
        Long memberCouponId = 55L;
        Long orderId = 123L;

        MemberCoupon mc = MemberCoupon.builder()
                .id(memberCouponId)
                .userId(userId)
                .status(Status.USED)
                .orderId(orderId)
                .build();

        when(memberCouponRepository.findById(memberCouponId)).thenReturn(Optional.of(mc));

        memberCouponService.cancelCouponUsage(userId, new MemberCouponCancelRequestDto(memberCouponId, orderId));

        Assertions.assertEquals(Status.ISSUED, mc.getStatus());
        Assertions.assertNull(mc.getOrderId());
    }

    @Test
    @DisplayName("GlobalExceptionHandler 테스트")
    void handleExceptionTest() {
        DuplicateCouponException ex = new DuplicateCouponException();

        ResponseEntity<ErrorResponse> res = globalExceptionHandler.handleCouponServerException(ex);

        Assertions.assertEquals(HttpStatus.CONFLICT, res.getStatusCode());
        Assertions.assertEquals(ErrorCode.DUPLICATE_COUPON_ISSUE.getCode(), res.getBody().getCode());
    }
}