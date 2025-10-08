package com.memorylab.domain;

public enum BoardStatus {
    // 변환 요청 및 진행 상태
    DISPATCHED,      // 변환 요청 완료
    PROCESSING,      // AI 서버에서 변환 중
    RESULT_READY,    // 변환 결과 URL 수신, 다운로드 대기
    DOWNLOADING,     // PLY 파일 다운로드 중
    READY,           // 3D 모델 표시 가능

    // 실패 및 예외 상태
    FAILED_POLL,     // 상태 확인 API 호출 실패
    FAILED_PROCESS,  // AI 서버에서 변환 실패
    FAILED_DOWNLOAD, // PLY 파일 다운로드 실패

    // 기타 상태
    CANCELLED;       // 사용자에 의해 취소됨

    /**
     * 해당 상태가 더 이상 변경되지 않는 최종 상태인지 확인합니다.
     * @return 최종 상태이면 true
     */
    public boolean isTerminal() {
        return this == READY ||
               this == FAILED_POLL ||
               this == FAILED_PROCESS ||
               this == FAILED_DOWNLOAD ||
               this == CANCELLED;
    }
}
