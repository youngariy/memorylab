/**
 * AI 서버 에러 코드를 사용자 친화적인 메시지로 매핑
 */

export const AI_ERROR_MESSAGES: Record<string, string> = {
  DOWNLOAD_FAILED: '동영상 다운로드 중 오류가 발생했습니다. 파일을 다시 업로드해 주세요.',
  MISSING_INPUT: '서버에 문제가 발생했습니다. 이 문제가 반복된다면 관리자에게 문의하세요.',
  GPU_ERROR: '업로드된 동영상이 변환에 적합하지 않습니다. 다른 영상을 시도해 주세요.',
  INVALID_EXTENSION: '지원하지 않는 파일 형식입니다. MP4 형식의 영상을 업로드해 주세요.',
  VIDEO_TOO_SHORT: '영상 길이가 너무 짧습니다. 최소 10초 이상의 영상을 업로드해 주세요.',
  UPLOAD_RETRY_EXHAUSTED: 'AI 서버와의 연결을 실패했습니다. 문의하기를 이용해주세요.',
};

/**
 * 에러 코드에 대응하는 사용자 친화적인 메시지를 반환
 * @param errorCode AI 서버에서 전달받은 에러 코드
 * @param fallbackMessage 매핑되지 않은 에러 코드의 기본 메시지
 * @returns 사용자에게 표시할 메시지
 */
export function getErrorMessage(errorCode: string | null, fallbackMessage?: string): string {
  if (!errorCode) {
    return fallbackMessage || '알 수 없는 오류가 발생했습니다. 다시 시도해 주세요.';
  }

  return AI_ERROR_MESSAGES[errorCode] || fallbackMessage || `오류가 발생했습니다 (${errorCode})`;
}

/**
 * 에러 코드가 매핑 테이블에 존재하는지 확인
 */
export function isKnownErrorCode(errorCode: string): boolean {
  return errorCode in AI_ERROR_MESSAGES;
}
