package com.nhnacademy.member_server.service.impl.member;

import com.nhnacademy.member_server.dto.request.member.AddressRequest;
import com.nhnacademy.member_server.dto.response.member.AddressListResponse;
import com.nhnacademy.member_server.dto.response.member.AddressResponse;
import com.nhnacademy.member_server.entity.member.Address;
import com.nhnacademy.member_server.entity.member.Member;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.repository.AddressRepository;
import com.nhnacademy.member_server.repository.MemberRepository;
import com.nhnacademy.member_server.service.member.AddressService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {
    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    @Override
    public AddressResponse findDefaultAddress(Long memberId) {
        log.info(">>> [조회] 기본 배송지 조회 요청 - MemberID: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getDefaultAddressId() == null) {
            log.warn(">>> [조회 결과] 설정된 기본 배송지가 없습니다. MemberID: {}", memberId);
            throw new BusinessException(ErrorCode.DEFAULT_ADDRESS_NOT_FOUND);
        }

        Address defaultAddress = addressRepository.findById(member.getDefaultAddressId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        log.info(">>> [조회 완료] 기본 배송지 ID: {}", defaultAddress.getId());
        return AddressResponse.from(defaultAddress, true);
    }

    @Transactional(readOnly = true)
    @Override
    public AddressListResponse findAddressList(Long memberId) {
        log.info(">>> [목록] 전체 배송지 목록 조회 - MemberID: {}", memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        List<Address> addressList = member.getAddresses();
        log.info(">>> [목록 결과] 조회된 주소 개수: {}개", addressList.size());

        List<AddressResponse> addressResponseList = new ArrayList<>();
        for (Address address : addressList) {
            boolean isDefault = Objects.equals(member.getDefaultAddressId(), address.getId());
            addressResponseList.add(AddressResponse.from(address, isDefault));
        }
        return new AddressListResponse(addressResponseList);
    }

    // ★ 에러 지점: 인터페이스와 이름/파라미터를 정확히 맞춰야 함
    @Transactional(readOnly = true)
    @Override
    public AddressResponse findAddress(Long memberId, Long addressId) {
        log.info(">>> [단건 조회] MemberID: {}, AddressID: {}", memberId, addressId);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if(!address.getMember().getId().equals(memberId)) {
            log.error(">>> [조회 실패] 권한 없음");
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }

        Member member = memberRepository.findById(memberId).get();
        boolean isDefault = Objects.equals(member.getDefaultAddressId(), address.getId());
        return AddressResponse.from(address, isDefault);
    }

    @Transactional
    @Override
    public AddressResponse registerAddress(Long memberId, AddressRequest addressRequest) {
        log.info(">>> [등록 시작] MemberID: {}, Alias: {}, isDefaultChecked: {}",
                memberId, addressRequest.getAlias(), addressRequest.isDefaultAddress());

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getAddresses().size() >= 10) {
            log.error(">>> [등록 실패] 최대 주소 개수 초과 (10개)");
            throw new BusinessException(ErrorCode.MAX_ADDRESS_LIMIT_EXCEEDED);
        }

        Address address = Address.builder()
                .alias(addressRequest.getAlias())
                .recipient(addressRequest.getRecipient())
                .phone(addressRequest.getPhone())
                .zipCode(addressRequest.getZipCode())
                .roadAddress(addressRequest.getRoadAddress())
                .detailAddress(addressRequest.getDetailAddress())
                .member(member)
                .build();

        addressRepository.save(address);
        member.addAddress(address);
        log.info(">>> [등록 완료] 새로운 주소 ID: {}", address.getId());

        boolean isDefault = false;
        if (member.getAddresses().size() == 1 || addressRequest.isDefaultAddress()) {
            log.info(">>> [등록-기본설정] 이 주소를 기본 배송지로 설정합니다.");
            member.setDefaultAddressId(address.getId());
            memberRepository.saveAndFlush(member);
            isDefault = true;
        }

        return AddressResponse.from(address, isDefault);
    }

    @Transactional
    @Override
    public AddressResponse modifyAddress(Long memberId, Long addressId, AddressRequest addressRequest) {
        log.info(">>> [수정 시작] MemberID: {}, AddressID: {}, isDefaultChecked: {}",
                memberId, addressId, addressRequest.isDefaultAddress());

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if(!address.getMember().getId().equals(memberId)) {
            log.error(">>> [수정 실패] 권한 없음. 요청자: {}, 소유자: {}", memberId, address.getMember().getId());
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }

        address.setRecipient(addressRequest.getRecipient());
        address.setPhone(addressRequest.getPhone());
        address.setZipCode(addressRequest.getZipCode());
        address.setAlias(addressRequest.getAlias());
        address.setRoadAddress(addressRequest.getRoadAddress());
        address.setDetailAddress(addressRequest.getDetailAddress());

        addressRepository.saveAndFlush(address);
        log.info(">>> [수정-주소] 주소 정보(도로명, 별칭 등) DB 반영 완료");

        if (addressRequest.isDefaultAddress()) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

            log.info(">>> [수정-기본변경] 기본 배송지 ID 업데이트 시도: {} -> {}", member.getDefaultAddressId(), addressId);
            member.setDefaultAddressId(addressId);
            memberRepository.saveAndFlush(member);
            log.info(">>> [수정-기본변경] Member 테이블 업데이트 완료");
        }

        boolean isDefaultResult = Objects.equals(address.getMember().getDefaultAddressId(), address.getId());
        log.info(">>> [수정 완료] 최종 기본 배송지 여부: {}", isDefaultResult);

        return AddressResponse.from(address, isDefaultResult);
    }

    @Transactional
    @Override
    public void removeAddress(Long memberId, Long addressId) {
        log.info(">>> [삭제 요청] MemberID: {}, AddressID: {}", memberId, addressId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if(!address.getMember().getId().equals(memberId)) {
            log.error(">>> [삭제 실패] 권한 없음");
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }

        if(Objects.equals(member.getDefaultAddressId(), addressId)) {
            log.info(">>> [삭제-기본해제] 기본 배송지를 삭제함에 따라 설정을 Null로 변경합니다.");
            member.setDefaultAddressId(null);
            memberRepository.saveAndFlush(member);
        }

        addressRepository.deleteById(addressId);
        log.info(">>> [삭제 완료] AddressID: {}", addressId);
    }

    @Transactional
    @Override
    public AddressResponse setDefaultAddress(Long memberId, Long addressId) {
        log.info(">>> [강제 기본설정] MemberID: {}, AddressID: {}", memberId, addressId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ADDRESS_ACCESS_DENIED);
        }

        member.setDefaultAddressId(addressId);
        memberRepository.saveAndFlush(member);
        log.info(">>> [강제 기본설정 완료] Member의 default_address_id 반영됨");

        return AddressResponse.from(address, true);
    }
}