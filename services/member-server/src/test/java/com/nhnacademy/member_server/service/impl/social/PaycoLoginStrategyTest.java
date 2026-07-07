package com.nhnacademy.member_server.service.impl.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.nhnacademy.member_server.dto.response.social.OAuth2UserInfo;
import com.nhnacademy.member_server.dto.response.social.PaycoMemberResponse;
import com.nhnacademy.member_server.dto.response.social.PaycoTokenResponse;
import com.nhnacademy.member_server.feign.PaycoApiFeignClient;
import com.nhnacademy.member_server.feign.PaycoAuthFeignClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaycoLoginStrategyTest {

    @InjectMocks
    PaycoLoginStrategy strategy;

    @Mock
    PaycoAuthFeignClient authClient;

    @Mock
    PaycoApiFeignClient apiClient;

    @Test
    void getProviderName_Success() {
        assertThat(strategy.getProviderName()).isEqualTo("PAYCO");
    }

    @Test
    void getUserInfo_Success() {
        String authCode = "test_code";
        String accessToken = "access_token";

        ReflectionTestUtils.setField(strategy, "clientId", "test_client_id");
        ReflectionTestUtils.setField(strategy, "clientSecret", "test_client_secret");

        PaycoTokenResponse tokenResponse = new PaycoTokenResponse();
        ReflectionTestUtils.setField(tokenResponse, "accessToken", accessToken);

        given(authClient.getToken(anyString(), anyString(), anyString(), anyString()))
                .willReturn(tokenResponse);

        PaycoMemberResponse memberResponse = new PaycoMemberResponse();

        PaycoMemberResponse.PaycoHeader header = new PaycoMemberResponse.PaycoHeader();
        ReflectionTestUtils.setField(header, "resultCode", 0);
        ReflectionTestUtils.setField(header, "isSuccessful", true);
        ReflectionTestUtils.setField(memberResponse, "header", header);

        PaycoMemberResponse.PaycoData data = new PaycoMemberResponse.PaycoData();
        PaycoMemberResponse.PaycoMember member = new PaycoMemberResponse.PaycoMember();
        ReflectionTestUtils.setField(member, "idNo", "payco_123");
        ReflectionTestUtils.setField(member, "name", "홍길동");
        ReflectionTestUtils.setField(member, "email", "test@payco.com");
        ReflectionTestUtils.setField(data, "member", member);
        ReflectionTestUtils.setField(memberResponse, "data", data);

        given(apiClient.getMemberInfo(anyString(), anyString())).willReturn(memberResponse);

        OAuth2UserInfo userInfo = strategy.getUserInfo(authCode);

        assertThat(userInfo.getProvider()).isEqualTo("PAYCO");
        assertThat(userInfo.getProviderId()).isEqualTo("payco_123");
        assertThat(userInfo.getName()).isEqualTo("홍길동");
    }

    @Test
    void getUserInfo_Fail_TokenError() {
        ReflectionTestUtils.setField(strategy, "clientId", "test_client_id");
        ReflectionTestUtils.setField(strategy, "clientSecret", "test_client_secret");

        given(authClient.getToken(anyString(), anyString(), anyString(), anyString()))
                .willReturn(null);

        assertThatThrownBy(() -> strategy.getUserInfo("code"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void getUserInfo_Fail_ApiError() {
        ReflectionTestUtils.setField(strategy, "clientId", "test_client_id");
        ReflectionTestUtils.setField(strategy, "clientSecret", "test_client_secret");

        PaycoTokenResponse tokenResponse = new PaycoTokenResponse();
        ReflectionTestUtils.setField(tokenResponse, "accessToken", "access");
        given(authClient.getToken(anyString(), anyString(), anyString(), anyString()))
                .willReturn(tokenResponse);

        PaycoMemberResponse memberResponse = new PaycoMemberResponse();
        PaycoMemberResponse.PaycoHeader header = new PaycoMemberResponse.PaycoHeader();
        ReflectionTestUtils.setField(header, "resultCode", 9999);
        ReflectionTestUtils.setField(header, "resultMessage", "Error");
        ReflectionTestUtils.setField(memberResponse, "header", header);

        given(apiClient.getMemberInfo(anyString(), anyString())).willReturn(memberResponse);

        assertThatThrownBy(() -> strategy.getUserInfo("code"))
                .isInstanceOf(RuntimeException.class);
    }
}