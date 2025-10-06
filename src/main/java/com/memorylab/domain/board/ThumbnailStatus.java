package com.memorylab.domain.board;

public enum ThumbnailStatus {
    NONE,       // 썸네일 대상 아님 (동영상 없음)
    PENDING,    // 썸네일 생성 대기 중
    GENERATING, // 썸네일 생성 중 (동기/비동기)
    READY,      // 썸네일 생성 완료
    FAILED      // 썸네일 생성 실패
}
