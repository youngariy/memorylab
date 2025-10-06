package com.memorylab.dto.internal;

/**
 * AI 서버로부터 변환 결과를 수신하기 위한 DTO입니다.
 */
public record ConversionCallbackRequest(
    Long boardId,      // 게시물 ID
    String status,     // 변환 상태 (예: "COMPLETED", "FAILED")
    String resultUrl,  // 변환 완료된 동영상의 URL
    String errorMessage // 실패 시 오류 메시지
) {}
