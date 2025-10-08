package com.memorylab.ai;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ExternalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiResultHandlerService {

    private final BoardRepository boardRepository;

    @Transactional
    public void queuePlyDownload(String taskId, String resultUrl) {
        boardRepository.findByAiTaskId(taskId).ifPresentOrElse(
            board -> {
                log.info("AI 작업 성공 콜백 수신. taskId={}, resultUrl={}", taskId, resultUrl);
                board.setExternalStatus(ExternalStatus.COMPLETED);
                board.setExternalResultUrl(resultUrl);
                log.info("PLY 파일 다운로드 큐에 등록: boardId={}, resultUrl={}", board.getId(), resultUrl);
                // 실제 다운로드 큐에 작업을 추가하는 로직 (예: SQS, RabbitMQ)
            },
            () -> log.warn("AI 콜백: taskId '{}'에 해당하는 게시물을 찾을 수 없습니다.", taskId)
        );
    }

    @Transactional
    public void processFailedTask(String taskId, String errorCode, String errorDetail) {
        boardRepository.findByAiTaskId(taskId).ifPresentOrElse(
            board -> {
                log.error("AI 작업 실패 콜백 수신. taskId={}, errorCode={}, errorDetail={}", taskId, errorCode, errorDetail);
                board.setExternalStatus(ExternalStatus.FAILED);
                board.setExternalErrorCode(errorCode);
                board.setExternalErrorDetail(errorDetail);
            },
            () -> log.warn("AI 콜백: taskId '{}'에 해당하는 게시물을 찾을 수 없습니다.", taskId)
        );
    }
}
