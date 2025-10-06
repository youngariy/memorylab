package com.memorylab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class RefreshToken {

    @Id
    @Column(length = 255)
    private String jti; // JWT ID (Primary Key)

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String tokenHash; // 토큰 해시

    @Column(nullable = false)
    private LocalDateTime expiresAt; // 만료 시각

    @Column(nullable = false)
    private boolean revoked = false; // 폐기 여부

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private RefreshToken(String jti, Long userId, String tokenHash, LocalDateTime expiresAt) {
        this.jti = jti;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public static RefreshToken of(String jti, Long userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(jti, userId, tokenHash, expiresAt);
    }
}
