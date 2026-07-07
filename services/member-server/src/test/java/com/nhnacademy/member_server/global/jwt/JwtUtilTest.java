package com.nhnacademy.member_server.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.nhnacademy.member_server.entity.member.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    JwtUtil jwtUtil;
    String secret = "testSecretKeyForJwtTestingPurposesOnly123456";
    Long accessTime = 1000L * 60;
    Long refreshTime = 1000L * 60 * 60;
    Key key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, accessTime, refreshTime);
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void createAccessToken_Success() {
        Long memberId = 1L;
        Role role = Role.USER;

        String token = jwtUtil.createAccessToken(memberId, role);

        assertThat(token).isNotNull();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo(memberId);

        String roleClaim = jwtUtil.getClaims(token).get("role", String.class);
        assertThat(roleClaim).isEqualTo("USER");
    }

    @Test
    void createRefreshToken_Success() {
        Long memberId = 1L;

        String token = jwtUtil.createRefreshToken(memberId);

        assertThat(token).isNotNull();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserId(token)).isEqualTo(memberId);
    }

    @Test
    void getRemainingTime_Success() {
        String token = jwtUtil.createAccessToken(1L, Role.USER);
        long remainingTime = jwtUtil.getRemainingTime(token);

        assertThat(remainingTime)
                .isPositive()
                .isLessThanOrEqualTo(accessTime);
    }

    @Test
    void validateToken_Fail_Expired() {
        Date past = new Date(System.currentTimeMillis() - 1000);
        String expiredToken = Jwts.builder()
                .setSubject("1")
                .setExpiration(past)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        boolean isValid = jwtUtil.validateToken(expiredToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_Fail_InvalidSignature() {
        String otherSecret = "differentSecretKeyForTestingInvalidSignature123";
        Key otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));

        String invalidToken = Jwts.builder()
                .setSubject("1")
                .setExpiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(otherKey, SignatureAlgorithm.HS256)
                .compact();

        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_Fail_Malformed() {
        String malformedToken = "eyJhbGciOiJIUzI1NiJ9.malformed.token";
        boolean isValid = jwtUtil.validateToken(malformedToken);

        assertThat(isValid).isFalse();
    }

    @Test
    void validateToken_Fail_Empty() {
        boolean isValid = jwtUtil.validateToken("");
        assertThat(isValid).isFalse();
    }

    @Test
    void getRemainingTime_Fail_InvalidToken() {
        long time = jwtUtil.getRemainingTime("invalid.token");
        assertThat(time).isZero();
    }
}