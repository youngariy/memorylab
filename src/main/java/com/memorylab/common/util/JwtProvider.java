package com.memorylab.common.util;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final SecretKey key;

    @Value("${jwt.access-token-minutes:60}")
    private long accessTokenMinutes;

    @Value("${jwt.refresh-token-days:7}")
    private long refreshTokenDays;

    // === roles 파라미터 추가 ===
    public String createAccessToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .claim("roles", roles) // === roles 클레임 추가 ===
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plus(refreshTokenDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public SecretKey getKey() {
        return key;
    }
}
