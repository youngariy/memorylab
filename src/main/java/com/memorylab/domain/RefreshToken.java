package com.memorylab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 512)
    private String tokenValue;

    @Column(nullable = false)
    private Instant expiryDate;

    @Builder
    public RefreshToken(Long userId, String tokenValue, Instant expiryDate) {
        this.userId = userId;
        this.tokenValue = tokenValue;
        this.expiryDate = expiryDate;
    }

    public void updateToken(String tokenValue, Instant expiryDate) {
        this.tokenValue = tokenValue;
        this.expiryDate = expiryDate;
    }
}
