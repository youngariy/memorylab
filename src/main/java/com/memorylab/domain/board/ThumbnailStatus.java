package com.memorylab.domain.board;

public enum ThumbnailStatus {
    NONE,       // 썸네일 대상 아님
    PENDING,    // 생성 대기 중
    GENERATING, // 생성 중
    READY,      // 생성 완료
    FAILED      // 생성 실패
}
