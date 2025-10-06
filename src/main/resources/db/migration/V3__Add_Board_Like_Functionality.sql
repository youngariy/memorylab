-- V3: Add board like functionality

-- 1. board 테이블에 좋아요 수를 저장할 like_count 컬럼 추가
ALTER TABLE board
    ADD COLUMN like_count INT NOT NULL DEFAULT 0 COMMENT '게시글 좋아요 수';

-- 2. 사용자와 게시글의 좋아요 관계를 저장할 board_like 테이블 생성
CREATE TABLE board_like (
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (board_id, user_id),
    CONSTRAINT fk_board_like_to_board FOREIGN KEY (board_id) REFERENCES board (id) ON DELETE CASCADE,
    CONSTRAINT fk_board_like_to_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);

-- 3. 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_board_like_user_id ON board_like (user_id);
