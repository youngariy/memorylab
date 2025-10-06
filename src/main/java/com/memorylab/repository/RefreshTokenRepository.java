package com.memorylab.repository;

import com.memorylab.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> { // 기본 키 타입을 Long -> String으로 변경

    /**
     * JWT ID (jti)를 기준으로 리프레시 토큰을 찾습니다.
     * @param jti JWT의 고유 식별자
     * @return RefreshToken 옵셔널 객체
     */
    Optional<RefreshToken> findByJti(String jti);
}
