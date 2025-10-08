package com.memorylab.service;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ExternalStatus;
import com.memorylab.dto.GpuWebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WebhookService {

    private final BoardRepository boardRepository;
    private final FileDownloadService fileDownloadService;

    /**
     * @deprecated This service is part of the old architecture and will be replaced by AiCallbackController.
     * This method is temporarily patched to resolve compilation errors.
     */
    @Deprecated
    public void processWebhook(GpuWebhookPayload payload) {
        Board board = boardRepository.findByAiTaskId(payload.getTaskId())
            .orElseThrow(() -> new IllegalArgumentException("No board found with AI task ID: " + payload.getTaskId()));

        ExternalStatus currentStatus = board.getExternalStatus();
        // 이미 최종 처리된 웹훅은 무시 (중복 수신 방지)
        if (currentStatus != null && currentStatus.isTerminal()) {
            log.warn("Webhook for already terminal AI task ID {} received. Current status: {}. Ignoring.", payload.getTaskId(), currentStatus);
            return;
        }

        switch (payload.getStatus()) {
            case "completed":
                log.info("Processing 'completed' webhook for boardId: {}. Result URL: {}", board.getId(), payload.getResultUrl());
                board.setExternalStatus(ExternalStatus.COMPLETED);
                board.setExternalResultUrl(payload.getResultUrl());
                // 파일 다운로드 서비스 호출 (비동기적으로 실행될 것임)
                // 이 로직은 AiResultHandlerService로 이전될 예정입니다.
                fileDownloadService.downloadPlyFile(board, payload.getResultUrl());
                break;

            case "failed":
                log.error("Processing 'failed' webhook for boardId: {}. Error: {} - {}",
                    board.getId(), payload.getErrorCode(), payload.getErrorDetail());
                board.setExternalStatus(ExternalStatus.FAILED);
                board.setExternalErrorCode(payload.getErrorCode());
                board.setExternalErrorDetail(payload.getErrorDetail());
                break;

            default:
                log.warn("Received webhook with unhandled status '{}' for AI task ID: {}", payload.getStatus(), payload.getTaskId());
                break;
        }
    }
}
