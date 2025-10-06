-- V5: Update refresh_token table for rotation and reuse detection

-- 기존 테이블 삭제 (구조가 완전히 변경되므로)
DROP TABLE IF EXISTS refresh_token;

-- 새로운 refresh_token 테이블 생성
CREATE TABLE refresh_token (
    jti VARCHAR(255) NOT NULL PRIMARY KEY COMMENT 'JWT 고유 식별자',
    user_id BIGINT NOT NULL COMMENT '토큰 소유자 ID',
    token_hash VARCHAR(255) NOT NULL COMMENT 'SHA-256으로 해시된 토큰 값',
    expires_at DATETIME(6) NOT NULL COMMENT '토큰 만료 시각',
    revoked BOOLEAN NOT NULL DEFAULT FALSE COMMENT '토큰 폐기 여부',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refresh_token_to_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);

-- 인덱스 추가
CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
