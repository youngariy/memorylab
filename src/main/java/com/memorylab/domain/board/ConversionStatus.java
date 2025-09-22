package com.memorylab.domain.board;

public enum ConversionStatus {
    /** 변환 대기 중 (원본 동영상 업로드 완료) */
    PENDING,

    /** AI 서버에서 3D 모델 변환 진행 중 */
    PROCESSING,

    /** 변환 성공 */
    COMPLETED,

    /** 변환 실패 */
    ERROR
}
