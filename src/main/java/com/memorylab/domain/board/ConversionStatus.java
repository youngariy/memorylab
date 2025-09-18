package com.memorylab.domain.board;

public enum ConversionStatus {
    NOT_STARTED,    // 변환 시작 전 (게시글만 작성된 상태)
    UPLOADED,       // 서버 업로드 완료 (변환 대기)
    PROCESSING,     // 3D 모델 변환 중
    COMPLETED,      // 변환 성공
    FAILED          // 변환 실패
}
