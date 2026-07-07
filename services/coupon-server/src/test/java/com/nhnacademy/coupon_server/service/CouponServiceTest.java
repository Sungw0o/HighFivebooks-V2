package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.request.CouponRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponCountDto;
import com.nhnacademy.coupon_server.dto.response.CouponResponseDto;
import com.nhnacademy.coupon_server.entity.Coupon;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.CouponStatus;
import com.nhnacademy.coupon_server.entity.state.CouponType;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.exception.CouponServerException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
import com.nhnacademy.coupon_server.repository.coupon.CouponRepository;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.impl.CouponServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponPolicyRepository couponPolicyRepository;
    @Mock
    private MemberCouponRepository memberCouponRepository;
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CouponServiceImpl couponService;

    @BeforeEach
    void setUp() {
        couponService = new CouponServiceImpl(
                couponPolicyRepository,
                couponRepository,
                memberCouponRepository,
                redisTemplate
        );
    }

    @Test
    @DisplayName("쿠폰 템플릿 생성 성공 - Redis 저장 로직 포함")
    void createCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Coupon Policy")
                .discountType(DiscountType.FIXED)
                .build();

        CouponRequestDto requestDto = CouponRequestDto.builder()
                .id(policyId)
                .couponName("Summer Sale")
                .issueCount(100)
                .issueStartAt(LocalDateTime.now())
                .issueEndAt(LocalDateTime.now().plusDays(7))
                .validPeriodDate(30)
                .build();

        Coupon mockCoupon = Coupon.builder()
                .id(100L)
                .couponPolicy(mockPolicy)
                .couponName(requestDto.getCouponName())
                .issueCount(100) // Redis 저장을 위해 수량 설정
                .issuedEndAt(requestDto.getIssueEndAt()) // 만료 시간 설정을 위해 종료일 설정
                .build();

        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(mockPolicy));
        when(couponRepository.save(any(Coupon.class))).thenReturn(mockCoupon);

        // [추가] Redis Mocking (NPE 방지 핵심)
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        CouponResponseDto responseDto = couponService.create(requestDto);

        // Then
        Assertions.assertNotNull(responseDto);
        assertEquals(100L, responseDto.getId());
        assertEquals("Summer Sale", responseDto.getCouponName());
        assertEquals(policyId, responseDto.getCouponPolicyId());

        verify(valueOperations).set("coupon:count:100", "100");
        // 만료 시간 설정 검증
        verify(redisTemplate).expireAt(eq("coupon:count:100"), any(java.util.Date.class));
    }

    @Test
    @DisplayName("모든 쿠폰 템플릿 조회 성공")
    void findAllCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy mockPolicy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Policy")
                .build();

        Coupon coupon1 = Coupon.builder()
                .id(1L)
                .couponPolicy(mockPolicy)
                .couponName("Coupon 1")
                .build();

        Coupon coupon2 = Coupon.builder()
                .id(2L)
                .couponPolicy(mockPolicy)
                .couponName("Coupon 2")
                .build();

        when(couponRepository.findAll()).thenReturn(List.of(coupon1, coupon2));

        List<CouponResponseDto> responseDtoList = couponService.findAll();

        assertEquals(2, responseDtoList.size());
        assertEquals("Coupon 1", responseDtoList.get(0).getCouponName());
        assertEquals("Coupon 2", responseDtoList.get(1).getCouponName());
        assertEquals(policyId, responseDtoList.get(0).getCouponPolicyId());
    }

    @Test
    @DisplayName("발급 가능한 쿠폰 조회 - 잔여 수량 계산 확인")
    void findIssuableCouponSuccess() {
        Long policyId = 1L;
        CouponPolicy policy = CouponPolicy.builder()
                .id(policyId)
                .name("Test Policy")
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();

        Coupon limitedCoupon = Coupon.builder()
                .id(1L)
                .couponPolicy(policy)
                .couponName("Limited Coupon")
                .issueCount(100)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        Coupon unlimitedCoupon = Coupon.builder()
                .id(2L)
                .couponPolicy(policy)
                .couponName("무제한 쿠폰")
                .issueCount(null)
                .issuedStartAt(LocalDateTime.now().minusDays(1))
                .issuedEndAt(LocalDateTime.now().plusDays(1))
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        List<Coupon> coupons = List.of(limitedCoupon, unlimitedCoupon);
        Page<Coupon> couponPage = new PageImpl<>(coupons);

        when(couponRepository.findIssuableCoupons(any(), eq(CouponPolicyStatus.ACTIVE), eq(CouponType.NORMAL), eq(pageable)))
                .thenReturn(couponPage);

        CouponCountDto countDto = mock(CouponCountDto.class);
        when(countDto.getCouponId()).thenReturn(1L); // 1번 쿠폰
        when(countDto.getCount()).thenReturn(10L);

        when(memberCouponRepository.countByCouponIds(anyList()))
                .thenReturn(List.of(countDto));

        Page<CouponResponseDto> result = couponService.findIssuableCoupons(pageable);

        List<CouponResponseDto> responseDtoList = result.getContent();
        assertEquals(2, responseDtoList.size());

        assertEquals("Limited Coupon", responseDtoList.get(0).getCouponName());
        assertEquals(90, responseDtoList.get(0).getRemainingCount());

        assertEquals("무제한 쿠폰", responseDtoList.get(1).getCouponName());
        Assertions.assertNull(responseDtoList.get(1).getRemainingCount());
    }

    @Test
    @DisplayName("쿠폰 목록 조회 시나리오 - 각 상황별(정상, 대기, 만료, 소진, 비활성) 상태 계산 검증")
    void getCoupons_StatusCalculation_Scenario() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // 1. [ACTIVE] 정상 발급 가능
        Coupon activeCoupon = createMockCoupon(1L, "정상 쿠폰", CouponPolicyStatus.ACTIVE,
                now.minusDays(1), now.plusDays(1), 100);

        // 2. [WAITING] 발급 대기
        Coupon waitingCoupon = createMockCoupon(2L, "대기 쿠폰", CouponPolicyStatus.ACTIVE,
                now.plusDays(1), now.plusDays(7), 100);

        // 3. [EXPIRED] 기간 만료
        Coupon expiredCoupon = createMockCoupon(3L, "만료 쿠폰", CouponPolicyStatus.ACTIVE,
                now.minusDays(10), now.minusDays(1), 100);

        // 4. [SOLD_OUT] 소진
        Coupon soldOutCoupon = createMockCoupon(4L, "소진 쿠폰", CouponPolicyStatus.ACTIVE,
                now.minusDays(1), now.plusDays(1), 10);

        // 5. [INACTIVE] 정책 비활성화
        Coupon inactiveCoupon = createMockCoupon(5L, "비활성 쿠폰", CouponPolicyStatus.INACTIVE,
                now.minusDays(1), now.plusDays(1), 100);

        List<Coupon> couponList = List.of(activeCoupon, waitingCoupon, expiredCoupon, soldOutCoupon, inactiveCoupon);
        Page<Coupon> couponPage = new PageImpl<>(couponList);

        when(couponRepository.findAll(any(Pageable.class))).thenReturn(couponPage);

        CouponCountDto countDto = mock(CouponCountDto.class);
        when(countDto.getCouponId()).thenReturn(4L);
        when(countDto.getCount()).thenReturn(10L);

        when(memberCouponRepository.countByCouponIds(anyList())).thenReturn(List.of(countDto));

        Page<CouponResponseDto> result = couponService.findAll(PageRequest.of(0, 10));

        List<CouponResponseDto> content = result.getContent();

        assertEquals("ACTIVE", content.get(0).getStatus());
        assertEquals("WAITING", content.get(1).getStatus());
        assertEquals("EXPIRED", content.get(2).getStatus());
        assertEquals("SOLD_OUT", content.get(3).getStatus());
        assertEquals("INACTIVE", content.get(4).getStatus());
    }

    private Coupon createMockCoupon(Long id, String name, CouponPolicyStatus policyStatus,
                                    LocalDateTime start, LocalDateTime end, Integer count) {
        CouponPolicy policy = CouponPolicy.builder()
                .status(policyStatus)
                .build();

        return Coupon.builder()
                .id(id)
                .couponName(name)
                .couponPolicy(policy)
                .issuedStartAt(start)
                .issuedEndAt(end)
                .issueCount(count)
                .couponType(CouponType.NORMAL)
                .build();
    }

    @Test
    @DisplayName("전체 쿠폰 조회 시 잔여 수량 계산 로직 검증")
    void findAll_RemainingCountCalculation() {
        Coupon normalCoupon = Coupon.builder()
                .id(1L)
                .couponName("정상 쿠폰")
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        Coupon overIssuedCoupon = Coupon.builder()
                .id(2L)
                .couponName("초과 발급된 쿠폰")
                .issueCount(100)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        Coupon unlimitedCoupon = Coupon.builder()
                .id(3L)
                .couponName("무제한 쿠폰")
                .issueCount(null)
                .couponPolicy(CouponPolicy.builder().status(CouponPolicyStatus.ACTIVE).build())
                .build();

        when(couponRepository.findAll()).thenReturn(List.of(normalCoupon, overIssuedCoupon, unlimitedCoupon));

        CouponCountDto countDto1 = mock(CouponCountDto.class);
        when(countDto1.getCouponId()).thenReturn(1L);
        when(countDto1.getCount()).thenReturn(30L);

        CouponCountDto countDto2 = mock(CouponCountDto.class);
        when(countDto2.getCouponId()).thenReturn(2L);
        when(countDto2.getCount()).thenReturn(120L);

        when(memberCouponRepository.countByCouponIds(anyList()))
                .thenReturn(List.of(countDto1, countDto2));

        List<CouponResponseDto> result = couponService.findAll();

        Assertions.assertEquals(3, result.size());

        CouponResponseDto normalDto = result.stream().filter(c -> c.getId().equals(1L)).findFirst().get();
        Assertions.assertEquals(70, normalDto.getRemainingCount());

        CouponResponseDto overIssuedDto = result.stream().filter(c -> c.getId().equals(2L)).findFirst().get();
        Assertions.assertEquals(0, overIssuedDto.getRemainingCount());

        CouponResponseDto unlimitedDto = result.stream().filter(c -> c.getId().equals(3L)).findFirst().get();
        Assertions.assertNull(unlimitedDto.getRemainingCount());
    }

    @Test
    @DisplayName("쿠폰 상태 변경 성공 (ACTIVE -> INACTIVE)")
    void updateCouponStatus_Success() {
        Long couponId = 1L;
        CouponStatus newStatus = CouponStatus.INACTIVE;

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .status(CouponStatus.ACTIVE)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        couponService.updateCouponStatus(couponId, newStatus);

        Assertions.assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
        verify(couponRepository, times(1)).findById(couponId);
    }

    @Test
    @DisplayName("쿠폰 상태 변경 실패 - 존재하지 않는 쿠폰")
    void updateCouponStatus_Fail_NotFound() {
        Long couponId = 999L;
        CouponStatus newStatus = CouponStatus.INACTIVE;

        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        CouponServerException exception = Assertions.assertThrows(CouponServerException.class, () -> {
            couponService.updateCouponStatus(couponId, newStatus);
        });

        Assertions.assertEquals(ErrorCode.COUPON_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("재발행 시나리오: INACTIVE -> ACTIVE 전환 시 Redis 재고 복원 여부")
    void updateCouponStatus_Reissue_RestoresRedis() {
        // Given
        Long couponId = 1L;
        // 기존 상태 INACTIVE
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .status(CouponStatus.INACTIVE)
                .issueCount(100)
                .issuedEndAt(LocalDateTime.now().plusDays(5))
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // Mock: Redis 키가 없는 상황 (만료됨)
        when(redisTemplate.hasKey("coupon:count:" + couponId)).thenReturn(false);
        // Mock: DB 발급 수량 20장 가정 (잔여 80장)
        when(memberCouponRepository.countByCouponId(couponId)).thenReturn(20L);

        // Redis Ops Mocking
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        couponService.updateCouponStatus(couponId, CouponStatus.ACTIVE);

        // Then
        // 1. 상태 변경 확인
        Assertions.assertEquals(CouponStatus.ACTIVE, coupon.getStatus());

        // 2. Redis 복구 로직 실행 확인 (잔여 80개 세팅)
        verify(valueOperations).set("coupon:count:" + couponId, "80");
        // 3. 만료 시간 설정 확인
        verify(redisTemplate).expire(eq("coupon:count:" + couponId), anyLong(), any());
    }

    @Test
    @DisplayName("재발행 시나리오: 이미 Redis 키가 존재하면 덮어쓰지 않음")
    void updateCouponStatus_Reissue_SkipIfRedisExists() {
        // Given
        Long couponId = 1L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .status(CouponStatus.INACTIVE)
                .issueCount(100)
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // Mock: Redis 키가 이미 존재함
        when(redisTemplate.hasKey("coupon:count:" + couponId)).thenReturn(true);

        // When
        couponService.updateCouponStatus(couponId, CouponStatus.ACTIVE);

        // Then
        Assertions.assertEquals(CouponStatus.ACTIVE, coupon.getStatus());

        // Redis set은 호출되지 않아야 함
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("멱등성: 동일 상태로 변경 시도 시 무시 (예외 발생 X, 로직 수행 X)")
    void updateCouponStatus_Idempotency() {
        // Given
        Long couponId = 1L;
        Coupon coupon = Coupon.builder()
                .id(couponId)
                .status(CouponStatus.ACTIVE) // 이미 ACTIVE
                .build();

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(coupon));

        // When
        couponService.updateCouponStatus(couponId, CouponStatus.ACTIVE);

        // Then
        // 상태 변경 로직이 실행되지 않았으므로 Redis 체크도 없어야 함
        verify(redisTemplate, never()).hasKey(anyString());
        // 상태는 그대로 유지
        Assertions.assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
    }

    @Test
    @DisplayName("상품 상세 페이지 - 적용 가능한 모든 쿠폰 조회 (전역 + 타겟)")
    void getCouponsForProduct_Success() {
        Long bookId = 100L;
        List<Long> categoryIds = List.of(10L, 20L);

        Coupon globalCoupon = Coupon.builder()
                .id(1L)
                .couponName("전역 쿠폰")
                .couponPolicy(CouponPolicy.builder().id(1L).build())
                .build();

        Coupon targetCoupon = Coupon.builder()
                .id(2L)
                .couponName("도서 타겟 쿠폰")
                .couponPolicy(CouponPolicy.builder().id(2L).build())
                .build();

        when(couponRepository.findCouponsForProduct(eq(bookId), eq(categoryIds), eq(CouponPolicyStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(globalCoupon, targetCoupon));

        List<CouponResponseDto> result = couponService.getCouponsForProduct(bookId, categoryIds);

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("전역 쿠폰", result.get(0).getCouponName());
        Assertions.assertEquals("도서 타겟 쿠폰", result.get(1).getCouponName());

        verify(couponRepository).findCouponsForProduct(eq(bookId), eq(categoryIds), eq(CouponPolicyStatus.ACTIVE), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("상품 전용 쿠폰 조회 - Redis 조회 로직 포함")
    void getBookSpecificCoupons_Success() {
        Long bookId = 100L;
        List<Long> categoryIds = List.of(10L);

        Coupon specificCoupon = Coupon.builder()
                .id(3L)
                .couponName("상품 전용 쿠폰")
                .couponPolicy(CouponPolicy.builder().id(3L).build())
                .issueCount(50)
                .build();

        when(couponRepository.findSpecificCouponsForProduct(eq(bookId), eq(categoryIds), eq(CouponPolicyStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(specificCoupon));

        // 서비스 코드 내부에서 redisTemplate.opsForValue().get()을 호출하므로 Mocking 필요
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("coupon:count:3")).thenReturn("45");

        List<CouponResponseDto> result = couponService.getBookSpecificCoupons(bookId, categoryIds);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("상품 전용 쿠폰", result.get(0).getCouponName());

        verify(redisTemplate.opsForValue()).get("coupon:count:3");
    }
}