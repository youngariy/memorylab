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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class VideoConversionScheduler {

    private static final String TRACE_ID_KEY = "traceId";

    private final BoardRepository boardRepository;
    private final BoardService boardService;
    private final TaskExecutor taskExecutor;

    @Value("${app.scheduler.max-retries:3}")
    private int maxRetries;

    @Value("${app.scheduler.batch-size:5}")
    private int batchSize;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public VideoConversionScheduler(BoardRepository boardRepository, BoardService boardService, @Qualifier("videoConversionTaskExecutor") TaskExecutor taskExecutor) {
        this.boardRepository = boardRepository;
        this.boardService = boardService;
        this.taskExecutor = taskExecutor;
    }

    @Scheduled(fixedRateString = "${app.scheduler.conversion-rate-ms:30000}")
    public void processVideoConversionQueue() {
        if (isRunning.getAndSet(true)) {
            log.warn("Scheduler is already running. Skipping this execution.");
            return;
        }

        // 스케줄러 실행을 위한 최상위 traceId 생성
        MDC.put(TRACE_ID_KEY, "scheduler-" + UUID.randomUUID().toString().substring(0, 8));

        try {
            log.info("Video conversion scheduler started. Batch size: {}", batchSize);

            Page<Board> pendingJobs = boardRepository.findByConversionStatusOrderByCreatedAtAsc(
                    ConversionStatus.PENDING,
                    PageRequest.of(0, batchSize)
            );

            if (pendingJobs.isEmpty()) {
                log.info("No pending conversion jobs found.");
                return;
            }

            log.info("Found {} pending jobs. Submitting to the thread pool...", pendingJobs.getNumberOfElements());

            for (Board board : pendingJobs.getContent()) {
                // MdcTaskDecorator가 현재 스레드의 MDC 컨텍스트를 복사하여 자식 스레드로 전파
                taskExecutor.execute(() -> {
                    try {
                        log.info("Processing job in a new thread: boardId={}, retryCount={}", board.getId(), board.getRetryCount());

                        if (board.getRetryCount() >= maxRetries) {
                            log.warn("BoardId={} has reached the max retry limit. Marking as dead-letter.", board.getId());
                            boardService.markAsDeadLetter(board.getId());
                        } else {
                            boardService.triggerAiConversion(board.getId());
                        }
                    } catch (Exception e) {
                        log.error("An unexpected error occurred in the conversion task for boardId={}.", board.getId(), e);
                    }
                });
            }

        } finally {
            isRunning.set(false);
            log.info("Video conversion scheduler finished.");
            // 스케줄러 스레드의 MDC 컨텍스트 정리
            MDC.clear();
        }
    }
}
