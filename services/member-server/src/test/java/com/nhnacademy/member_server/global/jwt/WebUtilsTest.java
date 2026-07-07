package com.nhnacademy.member_server.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WebUtilsTest {

    @Test
    void getToken_WithBearerPrefix() {
        String bearerToken = "Bearer actualTokenValue";
        String result = WebUtils.getToken(bearerToken);

        assertThat(result).isEqualTo("actualTokenValue");
    }

    @Test
    void getToken_WithoutBearerPrefix() {
        String token = "actualTokenValue";
        String result = WebUtils.getToken(token);

        assertThat(result).isEqualTo("actualTokenValue");
    }

    @Test
    void getToken_Null() {
        String result = WebUtils.getToken(null);

        assertThat(result).isNull();
    }

    @Test
    void getToken_Empty() {
        String result = WebUtils.getToken("");
        assertThat(result).isEmpty();
    }
}