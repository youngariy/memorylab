package com.memorylab.scheduler;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.TranscodeStatus;
import com.memorylab.service.TranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoConversionScheduler {

    private final BoardRepository boardRepository;
    private final TranscodeService transcodeService; // 새로운 TranscodeService 주입

    @Value("${app.scheduler.batch-size:5}")
    private int batchSize;

    @Value("${app.scheduler.max-retries:3}")
    private int maxRetries;

    private volatile boolean isSchedulerRunning = false;

    @Scheduled(fixedRateString = "${app.scheduler.conversion-rate-ms:30000}")
    public void processPendingConversions() {
        if (isSchedulerRunning) {
            log.warn("Video conversion scheduler is already running. Skipping this cycle.");
            return;
        }
        isSchedulerRunning = true;
        log.info("Starting video conversion processing scheduler...");

        try {
            Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());

            // PENDING 상태이고, 최대 재시도 횟수를 넘지 않은 게시물을 조회
            Page<Board> pendingBoards = boardRepository.findBoardsByTranscodeStatusAndRetryCountLessThan(
                    TranscodeStatus.PENDING, maxRetries, pageable
            );

            if (pendingBoards.isEmpty()) {
                log.info("No pending video conversions to process.");
                return;
            }

            log.info("Found {} boards to trigger AI conversion for.", pendingBoards.getTotalElements());
            for (Board board : pendingBoards) {
                try {
                    // TranscodeService를 통해 AI 변환 요청
                    transcodeService.triggerAiConversion(board.getId());
                } catch (Exception e) {
                    log.error("Error while triggering AI conversion for boardId: {}", board.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred in the video conversion scheduler.", e);
        } finally {
            isSchedulerRunning = false;
            log.info("Video conversion processing scheduler finished.");
        }
    }
}
