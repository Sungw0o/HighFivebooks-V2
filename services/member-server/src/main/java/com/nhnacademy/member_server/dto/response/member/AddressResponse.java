package com.nhnacademy.member_server.dto.response.member;

import com.nhnacademy.member_server.entity.member.Address;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    Long addressId;
    String alias;

    // [추가됨]
    String recipient;
    String phone;
    String zipCode;

    String roadAddress;
    String detailAddress;
    boolean isDefault; // [추가됨]

    // isDefault를 외부에서 계산해서 넣어주는 방식
    public static AddressResponse from(Address address, boolean isDefault) {
        return AddressResponse.builder()
                .addressId(address.getId())
                .alias(address.getAlias())
                // Entity에서 꺼내서 DTO에 담기
                .recipient(address.getRecipient())
                .phone(address.getPhone())
                .zipCode(address.getZipCode())
                .roadAddress(address.getRoadAddress())
                .detailAddress(address.getDetailAddress())
                .isDefault(isDefault)
                .build();
    }

    public static AddressResponse from(Address address) {
        return from(address, false);
    }
}