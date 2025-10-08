package com.memorylab.scheduler;

import com.memorylab.ai.AiResultHandlerService;
import com.memorylab.ai.dto.AiStatusInfo;
import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ExternalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiStatusScheduler {

    private final BoardRepository boardRepository;
    private final WebClient webClient;
    private final AiResultHandlerService aiResultHandlerService;

    @Value("${ai.server.base-url}")
    private String aiServerBaseUrl;

    @Scheduled(fixedDelay = 60000) // 1분에 한 번씩 실행
    public void pollAiServerStatus() {
        log.debug("AI 서버 상태 폴링 시작");
        List<Board> boardsToCheck = boardRepository.findByExternalStatusIn(List.of(ExternalStatus.QUEUED, ExternalStatus.PROCESSING));

        if (boardsToCheck.isEmpty()) {
            log.debug("확인할 AI 작업이 없습니다.");
            return;
        }

        boardsToCheck.forEach(this::checkStatusForBoard);
        log.debug("AI 서버 상태 폴링 완료");
    }

    private void checkStatusForBoard(Board board) {
        String taskId = board.getAiTaskId();
        if (taskId == null) {
            return;
        }

        webClient.get()
                .uri(aiServerBaseUrl + "/status?task-id=" + taskId)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("AI 상태 확인 API 호출 실패: boardId={}, taskId={}, status={}, body={}",
                                            board.getId(), taskId, response.statusCode(), errorBody);
                                    // HttpStatusCode를 HttpStatus로 변환하여 전달
                                    return handleApiError(board, HttpStatus.valueOf(response.statusCode().value()));
                                })
                )
                .bodyToMono(AiStatusInfo.class)
                // flatMap 내부에서 board 객체를 직접 사용하여 불필요한 DB 조회를 없앰
                .flatMap(statusInfo -> Mono.fromRunnable(() -> processStatusUpdate(board, statusInfo)))
                .onErrorResume(e -> {
                    log.error("AI 상태 확인 처리 중 오류 발생: boardId={}, taskId={}. Error: {}", board.getId(), taskId, e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }

    @Transactional
    public void processStatusUpdate(Board board, AiStatusInfo statusInfo) {
        log.info("AI 상태 확인 성공: boardId={}, taskId={}, status={}", board.getId(), board.getAiTaskId(), statusInfo.getStatus());
        switch (statusInfo.getStatus().toUpperCase()) {
            case "COMPLETED":
                board.setExternalStatus(ExternalStatus.COMPLETED);
                board.setExternalResultUrl(statusInfo.getTask().getResultUrl());
                aiResultHandlerService.queuePlyDownload(board.getAiTaskId(), board.getExternalResultUrl());
                break;
            case "FAILED":
                board.setExternalStatus(ExternalStatus.FAILED);
                board.setExternalErrorCode(statusInfo.getTask().getErrorCode());
                board.setExternalErrorDetail(statusInfo.getTask().getErrorDetail());
                break;
            case "PROCESSING":
                if (board.getExternalStatus() != ExternalStatus.PROCESSING) {
                    board.setExternalStatus(ExternalStatus.PROCESSING);
                }
                break;
            default:
                log.warn("알 수 없는 AI 상태 값: {}", statusInfo.getStatus());
        }
    }

    @Transactional
    public Mono<? extends Throwable> handleApiError(Board board, HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) {
            log.warn("AI 서버에서 작업을 찾을 수 없어(404) FAILED 처리합니다: boardId={}, taskId={}", board.getId(), board.getAiTaskId());
            board.setExternalStatus(ExternalStatus.FAILED);
            board.setExternalErrorCode("TASK_NOT_FOUND");
            board.setExternalErrorDetail("AI 서버에서 해당 작업을 찾을 수 없습니다.");
        }
        return Mono.error(new RuntimeException("AI API Error: " + status));
    }
}
