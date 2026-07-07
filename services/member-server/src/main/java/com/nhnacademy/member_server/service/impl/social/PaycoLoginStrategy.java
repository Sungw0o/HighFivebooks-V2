package com.nhnacademy.member_server.service.impl.social;

import com.nhnacademy.member_server.dto.response.social.OAuth2UserInfo;
import com.nhnacademy.member_server.dto.response.social.PaycoMemberResponse;
import com.nhnacademy.member_server.dto.response.social.PaycoTokenResponse;
import com.nhnacademy.member_server.exception.BusinessException;
import com.nhnacademy.member_server.exception.ErrorCode;
import com.nhnacademy.member_server.feign.PaycoApiFeignClient;
import com.nhnacademy.member_server.feign.PaycoAuthFeignClient;
import com.nhnacademy.member_server.service.social.SocialLoginStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaycoLoginStrategy implements SocialLoginStrategy {

    private final PaycoAuthFeignClient authClient;
    private final PaycoApiFeignClient apiClient;

    @Value("${payco.client-id}")
    private String clientId;

    @Value("${payco.client-secret}")
    private String clientSecret;

    @Override
    public String getProviderName() {
        return "PAYCO";
    }

    @Override
    public OAuth2UserInfo getUserInfo(String code) {


        PaycoTokenResponse tokenResponse = authClient.getToken(
                "authorization_code",
                clientId,
                clientSecret,
                code
        );

        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new BusinessException(ErrorCode.PAYCO_TOKEN_ISSUE_FAILED);
        }

        PaycoMemberResponse memberResponse = apiClient.getMemberInfo(
                clientId,
                tokenResponse.getAccessToken()
        );

        if (memberResponse.getHeader() != null && memberResponse.getHeader().getResultCode() != 0) {
            throw new BusinessException(ErrorCode.PAYCO_API_ERROR);
        }

        if (memberResponse.getData() == null || memberResponse.getData().getMember() == null) {
            throw new BusinessException(ErrorCode.PAYCO_MEMBER_INFO_EMPTY);
        }

        PaycoMemberResponse.PaycoMember memberData = memberResponse.getData().getMember();

        String birth = memberData.getBirthday();
        if (birth == null || birth.isBlank()) {
            birth = memberData.getBirthdayMMdd();
        }

        return OAuth2UserInfo.builder()
                .provider("PAYCO")
                .providerId(memberData.getIdNo())
                .name(memberData.getName())
                .email(memberData.getEmail())
                .mobile(memberData.getMobile())
                .gender(memberData.getGenderCode())
                .birthday(birth)
                .build();
    }
}