package com.nhnacademy.member_server.dto.response.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaycoTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("access_token_secret")
    private String accessTokenSecret;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    @JsonProperty("state")
    private String state;
}