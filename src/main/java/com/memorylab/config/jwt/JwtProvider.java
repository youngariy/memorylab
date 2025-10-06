package com.memorylab.config.jwt;

import com.memorylab.domain.user.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParserBuilder;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final SecretKey jwtSecretKey;
    private final JwtProperties jwtProperties;

    // 리프레시 토큰과 그 메타데이터를 함께 반환하는 record
    public record RefreshTokenMeta(String token, String jti, LocalDateTime expiresAt) {}

    public String createAccessToken(Member user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofMinutes(jwtProperties.getAccessTokenMinutes()));

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and() // audience 추가
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", user.getRoles())
                .signWith(jwtSecretKey)
                .compact();
    }

    public RefreshTokenMeta createRefreshTokenWithMeta(Member user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofDays(jwtProperties.getRefreshTokenDays()));
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and() // audience 추가
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .subject(user.getEmail())
                .id(jti) // 고유 식별자 (JTI) 추가
                .claim("typ", "refresh") // 토큰 타입을 'refresh'로 명시
                .signWith(jwtSecretKey)
                .compact();

        return new RefreshTokenMeta(token, jti, LocalDateTime.ofInstant(expiration, ZoneId.systemDefault()));
    }

    /**
     * 공통 검증 규칙이 설정된 JwtParserBuilder를 반환합니다.
     */
    public JwtParserBuilder parser() {
        return Jwts.parser()
                .verifyWith(jwtSecretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .setAllowedClockSkewSeconds(60); // 60초의 시계 오차 허용
    }
}
