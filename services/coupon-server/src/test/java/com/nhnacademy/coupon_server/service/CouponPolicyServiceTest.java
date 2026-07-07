package com.nhnacademy.coupon_server.service;

import com.nhnacademy.coupon_server.dto.request.CouponPolicyRequestDto;
import com.nhnacademy.coupon_server.dto.response.CouponPolicyResponseDto;
import com.nhnacademy.coupon_server.entity.CouponPolicy;
import com.nhnacademy.coupon_server.entity.MemberCoupon;
import com.nhnacademy.coupon_server.entity.state.Comment;
import com.nhnacademy.coupon_server.entity.state.CouponPolicyStatus;
import com.nhnacademy.coupon_server.entity.state.DiscountType;
import com.nhnacademy.coupon_server.entity.state.Status;
import com.nhnacademy.coupon_server.exception.CouponPolicyNotFoundException;
import com.nhnacademy.coupon_server.exception.ErrorCode;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyBookRepository;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyCategoryRepository;
import com.nhnacademy.coupon_server.repository.couponpolicy.CouponPolicyRepository;
import com.nhnacademy.coupon_server.repository.membercoupon.MemberCouponRepository;
import com.nhnacademy.coupon_server.service.impl.CouponPolicyServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CouponPolicyServiceTest {
    @Mock
    private CouponPolicyRepository couponPolicyRepository;
    @Mock
    private CouponPolicyBookRepository couponPolicyBookRepository;
    @Mock
    private CouponPolicyCategoryRepository couponPolicyCategoryRepository;
    @Mock
    private MemberCouponRepository memberCouponRepository;

    @InjectMocks
    private CouponPolicyServiceImpl couponPolicyService;

    private CouponPolicyRequestDto couponPolicyRequestDto;
    private CouponPolicy mockPolicy;

    @BeforeEach
    void setUp() {
        couponPolicyRequestDto = new CouponPolicyRequestDto();
        couponPolicyRequestDto.setName("신규 정책 테스트");
        couponPolicyRequestDto.setComment(Comment.WELCOME);
        couponPolicyRequestDto.setDiscountType(DiscountType.PERCENTAGE);
        couponPolicyRequestDto.setDiscountValue(10L);
        couponPolicyRequestDto.setMinOrderValue(10000L);
        couponPolicyRequestDto.setMaxDiscountValue(5000L);
        couponPolicyRequestDto.setTargetBookIds(List.of(1L, 2L));
        couponPolicyRequestDto.setTargetCategoryIds(List.of(10L));

        mockPolicy = CouponPolicy.builder()
                .id(1L)
                .name(couponPolicyRequestDto.getName())
                .comment(couponPolicyRequestDto.getComment())
                .discountType(couponPolicyRequestDto.getDiscountType())
                .discountValue(couponPolicyRequestDto.getDiscountValue())
                .minOrderValue(couponPolicyRequestDto.getMinOrderValue())
                .maxDiscountValue(couponPolicyRequestDto.getDiscountValue())
                .build();
    }

    @Test
    @DisplayName("새로운 쿠폰 정책 생성 성공 - 특정 도서 및 카테고리 지정 포함")
    void testCreatePolicyWithTarget() {
        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenReturn(mockPolicy);

        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);

        verify(couponPolicyRepository, times(1)).save(any(CouponPolicy.class));

        verify(couponPolicyBookRepository, times(1)).saveAll(any());
        verify(couponPolicyCategoryRepository, times(1)).saveAll(any());

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(1L, responseDto.getId());
        Assertions.assertEquals("신규 정책 테스트", responseDto.getName());
        Assertions.assertEquals(Comment.WELCOME, responseDto.getComment());
    }

    @Test
    @DisplayName("새로운 쿠폰 정책 생성 성공 - 적용 대상 없이 정책만 생성")
    void testCreatePolicyWithoutTarget() {
        couponPolicyRequestDto.setTargetBookIds(null);
        couponPolicyRequestDto.setTargetCategoryIds(List.of());

        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenReturn(mockPolicy);

        CouponPolicyResponseDto responseDto = couponPolicyService.create(couponPolicyRequestDto);
        verify(couponPolicyRepository, times(1)).save(any(CouponPolicy.class));

        verify(couponPolicyBookRepository, never()).saveAll(any());
        verify(couponPolicyCategoryRepository, never()).saveAll(any());

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(1L, responseDto.getId());
    }

    @Test
    @DisplayName("쿠폰 정책 생성 실패 - 정률 할인 100% 초과 예외 발생")
    void testCreatePolicyFail_InvalidPercentage() {
        couponPolicyRequestDto.setDiscountType(DiscountType.PERCENTAGE);
        couponPolicyRequestDto.setDiscountValue(101L); // 100% 초과 설정

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                couponPolicyService.create(couponPolicyRequestDto)
        );
    }

    @Test
    @DisplayName("쿠폰 정책 생성 - 정액(FIXED) 할인 시 최대 할인 금액이 없으면 할인 금액과 동일하게 설정")
    void testCreatePolicy_FixedType_MaxDiscountAutoSet() {
        couponPolicyRequestDto.setDiscountType(DiscountType.FIXED);
        couponPolicyRequestDto.setDiscountValue(5000L);
        couponPolicyRequestDto.setMaxDiscountValue(null); // 최대 할인 금액 미설정

        // ArgumentCaptor를 사용해 repository.save()에 넘겨진 객체를 포획
        ArgumentCaptor<CouponPolicy> policyCaptor = ArgumentCaptor.forClass(CouponPolicy.class);
        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        couponPolicyService.create(couponPolicyRequestDto);

        verify(couponPolicyRepository).save(policyCaptor.capture());
        CouponPolicy savedPolicy = policyCaptor.getValue();

        // 검증: 정액 할인은 maxDiscountValue가 discountValue(5000)와 같아야 함
        Assertions.assertEquals(5000L, savedPolicy.getMaxDiscountValue());
    }

    @Test
    @DisplayName("쿠폰 정책 생성 - 정률(PERCENTAGE) 할인 시 최대 할인 금액이 0이면 Null로 설정")
    void testCreatePolicy_PercentageType_MaxDiscountNull() {
        couponPolicyRequestDto.setDiscountType(DiscountType.PERCENTAGE);
        couponPolicyRequestDto.setDiscountValue(20L);
        couponPolicyRequestDto.setMaxDiscountValue(0L); // 0으로 입력됨

        ArgumentCaptor<CouponPolicy> policyCaptor = ArgumentCaptor.forClass(CouponPolicy.class);
        when(couponPolicyRepository.save(any(CouponPolicy.class))).thenAnswer(invocation -> invocation.getArgument(0));

        couponPolicyService.create(couponPolicyRequestDto);

        verify(couponPolicyRepository).save(policyCaptor.capture());
        CouponPolicy savedPolicy = policyCaptor.getValue();

        // 검증: 정률 할인은 maxDiscountValue가 0일 경우 로직에 의해 null로 저장되어야 함 (무제한 의미)
        Assertions.assertNull(savedPolicy.getMaxDiscountValue());
    }

    @Test
    @DisplayName("쿠폰 정책 전체 조회 성공")
    void testFindAllSuccess(){
        CouponPolicy policy1 = CouponPolicy.builder()
                .id(1L)
                .name("정책1")
                .comment(Comment.WELCOME)
                .discountType(DiscountType.FIXED)
                .discountValue(1000L)
                .build();
        CouponPolicy policy2 = CouponPolicy.builder()
                .id(2L)
                .name("정책2")
                .comment(Comment.EVENT)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(10L)
                .build();

        when(couponPolicyRepository.findAll()).thenReturn(List.of(policy1, policy2));
        List<CouponPolicyResponseDto> responseDtoList = couponPolicyService.findAll();
        Assertions.assertNotNull(responseDtoList);
        Assertions.assertEquals(2, responseDtoList.size());

        Assertions.assertEquals("정책1", responseDtoList.get(0).getName());
        Assertions.assertEquals("정책2", responseDtoList.get(1).getName());

        verify(couponPolicyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 성공")
    void testFindByIdSuccess(){
        Long id = 1L;
        when(couponPolicyRepository.findById(id)).thenReturn(Optional.of(mockPolicy));

        CouponPolicyResponseDto responseDto = couponPolicyService.findById(id);

        Assertions.assertNotNull(responseDto);
        Assertions.assertEquals(id, responseDto.getId());
        Assertions.assertEquals(mockPolicy.getName(), responseDto.getName());

        verify(couponPolicyRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("쿠폰 정책 단건 조회 실패")
    void testFindByIdFailure(){
        Long id = 999L;
        when(couponPolicyRepository.findById(id)).thenReturn(Optional.empty());

        CouponPolicyNotFoundException exception = Assertions.assertThrows(
                CouponPolicyNotFoundException.class,
                () -> couponPolicyService.findById(id)
        );

        Assertions.assertEquals(ErrorCode.COUPON_POLICY_NOT_FOUND, exception.getErrorCode());

        verify(couponPolicyRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("쿠폰 정책 비활성화 성공")
    void testDeleteSuccess(){
        Long id = 1L;
        when(couponPolicyRepository.findById(id)).thenReturn(Optional.of(mockPolicy));

        couponPolicyService.deleteById(id);

        verify(couponPolicyRepository, times(1)).findById(id);
        verify(couponPolicyRepository, never()).deleteById(id);
        Assertions.assertEquals(CouponPolicyStatus.INACTIVE, mockPolicy.getStatus());
    }

    @Test
    @DisplayName("쿠폰 정책 삭제 실패 - 존재하지 않는 ID")
    void testDeleteFailureNotFound(){
        Long id = 999L;
        when(couponPolicyRepository.findById(id)).thenReturn(Optional.empty());

        CouponPolicyNotFoundException exception = Assertions.assertThrows(
                CouponPolicyNotFoundException.class,
                () -> couponPolicyService.deleteById(id)
        );

        Assertions.assertEquals(ErrorCode.COUPON_POLICY_NOT_FOUND, exception.getErrorCode());

        verify(couponPolicyRepository, never()).deleteById(id);
    }

    @Test
    @DisplayName("쿠폰 정책 삭제(비활성화) 시 관련 발급 쿠폰 일괄 만료 처리 검증")
    void testDeleteById_ShouldExpireIssuedCoupons() {
        Long policyId = 1L;

        // 1. 삭제할 정책 준비 (Active 상태)
        CouponPolicy policyToDelete = CouponPolicy.builder()
                .id(policyId)
                .status(CouponPolicyStatus.ACTIVE)
                .build();

        // 2. 해당 정책으로 발급된 쿠폰 리스트 준비 (ISSUED 상태)
        MemberCoupon coupon1 = MemberCoupon.builder()
                .id(101L)
                .status(Status.ISSUED)
                .build();

        MemberCoupon coupon2 = MemberCoupon.builder()
                .id(102L)
                .status(Status.ISSUED)
                .build();

        List<MemberCoupon> issuedCoupons = List.of(coupon1, coupon2);

        // 3. Mocking
        when(couponPolicyRepository.findById(policyId)).thenReturn(Optional.of(policyToDelete));
        when(memberCouponRepository.findAllByCouponCouponPolicyIdAndStatus(policyId, Status.ISSUED))
                .thenReturn(issuedCoupons);

        couponPolicyService.deleteById(policyId);

        // A. 정책 상태가 비활성화(INACTIVE) 되었는지 검증
        Assertions.assertEquals(CouponPolicyStatus.INACTIVE, policyToDelete.getStatus());

        // B. 발급된 쿠폰들의 상태가 모두 만료(EXPIRED)로 변경되었는지 검증 (핵심 로직)
        Assertions.assertEquals(Status.EXPIRED, coupon1.getStatus(), "첫 번째 쿠폰이 만료 처리되어야 합니다.");
        Assertions.assertEquals(Status.EXPIRED, coupon2.getStatus(), "두 번째 쿠폰이 만료 처리되어야 합니다.");

        // C. Repository 조회 메서드 호출 검증
        verify(memberCouponRepository, times(1)).findAllByCouponCouponPolicyIdAndStatus(policyId, Status.ISSUED);
    }

}