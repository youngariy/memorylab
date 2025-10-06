package com.memorylab.scheduler;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ThumbnailStatus;
import com.memorylab.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailScheduler {

    private final BoardRepository boardRepository;
    private final ThumbnailService thumbnailService;

    @Value("${app.scheduler.thumbnail.batch-size:10}")
    private int batchSize;

    @Value("${app.scheduler.thumbnail.max-retries:3}")
    private int maxRetries;

    private volatile boolean isRunning = false;

    @Scheduled(fixedRateString = "${app.scheduler.thumbnail.rate-ms:30000}") // 기본 30초
    public void processPendingThumbnails() {
        if (isRunning) {
            log.warn("Thumbnail scheduler is already running. Skipping this cycle.");
            return;
        }
        isRunning = true;
        log.info("Starting thumbnail processing scheduler...");

        try {
            Pageable pageable = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
            List<ThumbnailStatus> targetStatuses = List.of(ThumbnailStatus.PENDING, ThumbnailStatus.FAILED);

            Page<Board> pendingBoards = boardRepository.findBoardsByThumbnailStatusInAndRetryCountLessThan(
                    targetStatuses, maxRetries, pageable
            );

            if (pendingBoards.isEmpty()) {
                log.info("No pending thumbnails to process.");
                return;
            }

            log.info("Found {} boards to generate thumbnails for.", pendingBoards.getTotalElements());
            for (Board board : pendingBoards) {
                try {
                    thumbnailService.generateThumbnailAsync(board.getId());
                } catch (Exception e) {
                    log.error("Error while queuing async thumbnail generation for boardId: {}", board.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("An unexpected error occurred in the thumbnail scheduler.", e);
        } finally {
            isRunning = false;
            log.info("Thumbnail processing scheduler finished.");
        }
    }
}
