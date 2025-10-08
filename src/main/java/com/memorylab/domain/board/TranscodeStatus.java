package com.memorylab.domain.board;

public enum TranscodeStatus {
    NONE,       // 변환 대상 아님
    PENDING,    // 변환 대기 중
    CONVERTING, // 변환 중
    READY,      // 변환 완료
    FAILED      // 변환 실패
}
