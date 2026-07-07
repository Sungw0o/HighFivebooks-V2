package com.nhnacademy.member_server.global.jwt;


import com.nhnacademy.member_server.entity.member.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final Long accessExpirationTime;
    private final Long refreshExpirationTime;
    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration_time}") Long accessExpirationTime,
                   @Value("${jwt.refresh_expiration_time}") Long refreshExpirationTime) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationTime = accessExpirationTime;
        this.refreshExpirationTime = refreshExpirationTime;
    }

    //여기서는 Pk로 넣어줘야함 그래야 토큰에서 빼와서 x user id로 검증
    public String createAccessToken(Long memberId, Role role) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + accessExpirationTime);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("role", role.name())
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public long getRemainingTime(String token) {
        try {
            Date expiration = getClaims(token).getExpiration();
            long now = new Date().getTime();
            return expiration.getTime() - now;
        } catch (Exception e) {
            return 0;
        }
    }

    public String createRefreshToken(Long memberId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationTime);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

    }


    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            // log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            // log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            // log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            // log.error("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

}
