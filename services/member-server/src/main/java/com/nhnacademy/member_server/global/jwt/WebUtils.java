package com.nhnacademy.member_server.global.jwt;

public class WebUtils {
    public static String getToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return bearerToken;
    }
}