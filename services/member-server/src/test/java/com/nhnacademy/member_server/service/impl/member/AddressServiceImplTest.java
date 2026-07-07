package com.nhnacademy.member_server.service.impl.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.nhnacademy.member_server.dto.request.member.AddressRequest;
import com.nhnacademy.member_server.dto.response.member.AddressResponse;
import com.nhnacademy.member_server.entity.member.Address;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.AddressRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @InjectMocks
    AddressServiceImpl addressService;

    @Mock
    MemberRepository memberRepository;

    @Mock
    AddressRepository addressRepository;

    @Test
    @DisplayName("기본 배송지 조회 성공")
    void findDefaultAddressSuccessTest() {
        Long memberId = 1L;
        Long addressId = 10L;
        Member member = Member.builder().id(memberId).build();
        member.setDefaultAddressId(addressId);

        Address address = Address.builder().id(addressId).alias("Home").member(member).build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

        AddressResponse result = addressService.findDefaultAddress(memberId);

        assertThat(result.getAddressId()).isEqualTo(addressId);
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    @DisplayName("기본 배송지 조회 실패 - 설정된 기본 배송지 없음")
    void findDefaultAddressFailTest() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> addressService.findDefaultAddress(memberId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DEFAULT_ADDRESS_NOT_FOUND);
    }

    @Test
    @DisplayName("배송지 목록 조회 성공")
    void findAddressListTest() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).build();
        Address address = Address.builder().id(10L).member(member).build();
        member.addAddress(address);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        var result = addressService.findAddressList(memberId);

        assertThat(result.getAddressList()).hasSize(1);
    }

    @Test
    @DisplayName("특정 배송지 조회 성공")
    void findAddressSuccessTest() {
        Long memberId = 1L;
        Long addressId = 10L;
        Member member = Member.builder().id(memberId).build();
        Address address = Address.builder().id(addressId).member(member).build();

        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        AddressResponse result = addressService.findAddress(memberId, addressId);

        assertThat(result.getAddressId()).isEqualTo(addressId);
    }

    @Test
    @DisplayName("특정 배송지 조회 실패 - 권한 없음")
    void findAddressAccessDeniedTest() {
        Long memberId = 1L;
        Long otherMemberId = 2L;
        Long addressId = 10L;

        Member owner = Member.builder().id(otherMemberId).build();
        Address address = Address.builder().id(addressId).member(owner).build();

        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

        assertThatThrownBy(() -> addressService.findAddress(memberId, addressId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_ACCESS_DENIED);
    }

    @Test
    @DisplayName("배송지 등록 성공 - 첫 배송지는 기본 배송지로 설정")
    void registerAddressFirstTimeTest() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).addresses(new ArrayList<>()).build();

        AddressRequest request = AddressRequest.builder()
                .alias("Home")
                .recipient("Me")
                .phone("010-1234-5678")
                .zipCode("12345")
                .roadAddress("Road")
                .detailAddress("Detail")
                .defaultAddress(false)
                .build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(addressRepository.save(any(Address.class))).willAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 100L);
            return saved;
        });

        AddressResponse response = addressService.registerAddress(memberId, request);

        assertThat(response.isDefault()).isTrue();
        assertThat(member.getDefaultAddressId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("배송지 등록 실패 - 최대 개수 초과")
    void registerAddressLimitExceededTest() {
        Long memberId = 1L;
        Member member = Member.builder().id(memberId).addresses(new ArrayList<>()).build();
        for (int i = 0; i < 10; i++) {
            member.addAddress(Address.builder().build());
        }

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        AddressRequest request = new AddressRequest();

        assertThatThrownBy(() -> addressService.registerAddress(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_ADDRESS_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("배송지 수정 성공")
    void modifyAddressTest() {
        Long memberId = 1L;
        Long addressId = 10L;
        Member member = Member.builder().id(memberId).build();
        Address address = Address.builder().id(addressId).member(member).alias("Old").build();

        AddressRequest request = AddressRequest.builder()
                .alias("New")
                .recipient("New Recipient")
                .phone("010-9999-9999")
                .zipCode("54321")
                .roadAddress("New Road")
                .detailAddress("New Detail")
                .defaultAddress(true)
                .build();

        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        AddressResponse response = addressService.modifyAddress(memberId, addressId, request);

        assertThat(response.getAlias()).isEqualTo("New");
        assertThat(member.getDefaultAddressId()).isEqualTo(addressId);
    }

    @Test
    @DisplayName("배송지 삭제 성공 - 기본 배송지 삭제 시 null 처리")
    void removeAddressSuccessTest() {
        Long memberId = 1L;
        Long addressId = 10L;
        Member member = Member.builder().id(memberId).defaultAddressId(addressId).build();
        Address address = Address.builder().id(addressId).member(member).build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

        addressService.removeAddress(memberId, addressId);

        assertThat(member.getDefaultAddressId()).isNull();
        verify(addressRepository).deleteById(addressId);
    }

    @Test
    @DisplayName("배송지 삭제 실패 - 권한 없음")
    void removeAddressAccessDeniedTest() {
        Long memberId = 1L;
        Long otherMemberId = 2L;
        Long addressId = 10L;

        Member member = Member.builder().id(memberId).build();
        Member owner = Member.builder().id(otherMemberId).build();
        Address address = Address.builder().id(addressId).member(owner).build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

        assertThatThrownBy(() -> addressService.removeAddress(memberId, addressId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ADDRESS_ACCESS_DENIED);
    }

    @Test
    @DisplayName("기본 배송지 설정 성공")
    void setDefaultAddressTest() {
        Long memberId = 1L;
        Long addressId = 10L;
        Member member = Member.builder().id(memberId).build();
        Address address = Address.builder().id(addressId).member(member).build();

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(addressRepository.findById(addressId)).willReturn(Optional.of(address));

        AddressResponse response = addressService.setDefaultAddress(memberId, addressId);

        assertThat(response.isDefault()).isTrue();
        assertThat(member.getDefaultAddressId()).isEqualTo(addressId);
    }
}