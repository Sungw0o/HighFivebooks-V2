package com.nhnacademy.member_server.dto.request.member;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AddressRequest {
    @NotBlank(message = "별칭은 필수입니다")
    private String alias;

    @NotBlank(message = "받는 분 성함은 필수입니다")
    private String recipient;

    @NotBlank(message = "연락처는 필수입니다")
    private String phone;

    @NotBlank(message = "우편번호는 필수입니다")
    private String zipCode;

    @NotBlank(message = "도로명 주소는 필수입니다")
    private String roadAddress;

    @NotBlank(message = "상세 주소는 필수입니다")
    private String detailAddress;

    // 기본 배송지 여부
    private boolean defaultAddress;
}