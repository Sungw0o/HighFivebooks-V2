package com.nhnacademy.member_server.dto.response.social;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2UserInfo {
    private String provider;
    private String providerId;
    private String name;
    private String email;
    private String mobile;
    private String gender;
    private String birthday;
}