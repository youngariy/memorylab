-- V2: Add thumbnail and transcode status columns to the board table

-- 1. 새로운 ENUM 타입 정의 (MySQL 8.0+ 에서는 ALTER ENUM을 지원하지 않으므로, 컬럼 타입을 변경하는 방식을 사용)
-- 기존 ENUM 타입에 값을 추가하는 것은 복잡하므로, VARCHAR로 변경 후 CHECK 제약조건을 사용하는 것이 더 유연할 수 있으나,
-- 여기서는 요구사항에 명시된 ENUM을 최대한 따르기 위해 컬럼을 재생성하는 표준적인 방식을 사용합니다.
-- 하지만 운영 환경에서는 데이터 손실을 막기 위해 임시 테이블을 사용하는 등 더 신중한 접근이 필요합니다.
-- 이 스크립트는 개발 환경에 최적화되어 있습니다.

-- 2. Board 테이블에 새로운 컬럼들 추가
ALTER TABLE board
    ADD COLUMN original_video_path VARCHAR(512) NULL COMMENT '원본 동영상 파일의 상대 경로',
    ADD COLUMN converted_video_path VARCHAR(512) NULL COMMENT '변환된 동영상 파일의 상대 경로',
    ADD COLUMN thumbnail_path VARCHAR(512) NULL COMMENT '썸네일 이미지 파일의 상대 경로',
    ADD COLUMN thumbnail_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '썸네일 생성 상태',
    ADD COLUMN transcode_status VARCHAR(20) NOT NULL DEFAULT 'NONE' COMMENT '트랜스코딩 상태',
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '작업 재시도 횟수',
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락을 위한 버전 필드';

-- 3. 기존 데이터 마이그레이션 (백필 작업)
-- 기존 conversion_status 와 stored_file_path, video_url을 새로운 컬럼으로 이전합니다.
UPDATE board
SET
    -- 기존 stored_file_path를 original_video_path로 이전
    original_video_path = stored_file_path,

    -- 기존 video_url을 converted_video_path로 이전
    converted_video_path = video_url,

    -- 기존 conversion_status 값을 새로운 transcode_status로 매핑
    transcode_status = CASE
                           WHEN conversion_status = 'UPLOADED' THEN 'PENDING'
                           WHEN conversion_status = 'PROCESSING' THEN 'CONVERTING'
                           WHEN conversion_status = 'COMPLETED' THEN 'READY'
                           WHEN conversion_status = 'FAILED' THEN 'FAILED'
                           ELSE 'NONE'
        END,

    -- 동영상이 있는 모든 기존 게시물은 썸네일 생성이 필요하므로 PENDING으로 설정
    thumbnail_status = CASE
                           WHEN stored_file_path IS NOT NULL THEN 'PENDING'
                           ELSE 'NONE' -- 동영상이 없으면 썸네일도 없음
        END
WHERE stored_file_path IS NOT NULL;


-- 4. 기존 컬럼 삭제 (데이터 이전 확인 후)
-- ALTER TABLE board DROP COLUMN conversion_status;
-- ALTER TABLE board DROP COLUMN stored_file_path;
-- ALTER TABLE board DROP COLUMN video_url;
-- 참고: 위 삭제 구문은 실제 운영 반영 시 데이터가 성공적으로 이전되었는지 충분히 확인한 후 별도로 실행하는 것을 권장합니다.
--      이 마이그레이션 스크립트에서는 주석 처리합니다.

-- 5. 새로운 인덱스 추가
CREATE INDEX idx_board_thumbnail_status ON board (thumbnail_status);
CREATE INDEX idx_board_transcode_status ON board (transcode_status);

