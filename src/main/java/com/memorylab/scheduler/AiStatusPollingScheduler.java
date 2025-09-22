package com.memorylab.scheduler;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.ConversionStatus;
import com.memorylab.repository.board.BoardRepository;
import com.memorylab.service.board.BoardService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class AiStatusPollingScheduler {

    private static final String TRACE_ID_KEY = "traceId";

    private final BoardRepository boardRepository;
    private final BoardService boardService;
    private final TaskExecutor taskExecutor;

    @Value("${app.scheduler.polling.stale-threshold-minutes:5}")
    private long staleThresholdMinutes;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public AiStatusPollingScheduler(BoardRepository boardRepository, BoardService boardService, @Qualifier("videoConversionTaskExecutor") TaskExecutor taskExecutor) {
        this.boardRepository = boardRepository;
        this.boardService = boardService;
        this.taskExecutor = taskExecutor;
    }

    /**
     * PROCESSING 상태에 너무 오래 머물러 있는 작업을 폴링하여 상태를 확인합니다. (콜백 유실 대비 안전망)
     */
    @Scheduled(fixedRateString = "${app.scheduler.polling.rate-ms:60000}")
    public void pollStaleProcessingJobs() {
        if (isRunning.getAndSet(true)) {
            log.warn("[POLLING] Scheduler is already running. Skipping this execution.");
            return;
        }

        MDC.put(TRACE_ID_KEY, "polling-" + UUID.randomUUID().toString().substring(0, 8));

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleThresholdMinutes);
            log.info("[POLLING] Checking for jobs in PROCESSING state updated before {}.", threshold);

            // 한 번에 너무 많은 작업을 가져오지 않도록 페이징 처리 (여기서는 10개로 제한)
            Page<Board> staleJobs = boardRepository.findByConversionStatusAndUpdatedAtBefore(
                    ConversionStatus.PROCESSING,
                    threshold,
                    PageRequest.of(0, 10)
            );

            if (staleJobs.isEmpty()) {
                log.info("[POLLING] No stale processing jobs found.");
                return;
            }

            log.info("[POLLING] Found {} stale jobs. Submitting to the thread pool...", staleJobs.getNumberOfElements());

            for (Board board : staleJobs.getContent()) {
                taskExecutor.execute(() -> {
                    try {
                        log.warn("[POLLING] Job (boardId={}) is stale. Polling AI server for status.", board.getId());
                        boardService.checkConversionStatus(board.getId());
                    } catch (Exception e) {
                        log.error("[POLLING] An unexpected error occurred while polling for boardId={}.", board.getId(), e);
                    }
                });
            }

        } finally {
            isRunning.set(false);
            log.info("[POLLING] Polling scheduler finished.");
            MDC.clear();
        }
    }
}
