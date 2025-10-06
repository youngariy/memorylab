-- V4: Add comment functionality

-- 1. board 테이블에 댓글 수를 저장할 comment_count 컬럼 추가
ALTER TABLE board
    ADD COLUMN comment_count INT NOT NULL DEFAULT 0 COMMENT '게시글 댓글 수';

-- 2. 댓글 정보를 저장할 comment 테이블 생성
CREATE TABLE comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    modified_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_comment_to_board FOREIGN KEY (board_id) REFERENCES board (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_to_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
);

-- 3. 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_comment_board_id ON comment (board_id);
CREATE INDEX idx_comment_user_id ON comment (user_id);
