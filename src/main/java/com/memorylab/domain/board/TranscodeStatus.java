package com.memorylab.domain.board;

public enum TranscodeStatus {
    NONE,       // 트랜스코딩 대상 아님 (동영상 없음)
    PENDING,    // 트랜스코딩 대기 중
    CONVERTING, // 트랜스코딩 진행 중
    READY,      // 트랜스코딩 완료
    FAILED      // 트랜스코딩 실패
}
