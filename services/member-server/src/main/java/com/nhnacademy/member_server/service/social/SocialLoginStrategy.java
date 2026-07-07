package com.nhnacademy.member_server.service.social;

import com.nhnacademy.member_server.dto.response.social.OAuth2UserInfo;

public interface SocialLoginStrategy {
    String getProviderName();
    OAuth2UserInfo getUserInfo(String code);
}