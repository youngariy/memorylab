package com.memorylab.scheduler;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ThumbnailStatus;
import com.memorylab.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThumbnailScheduler {

    private final BoardRepository boardRepository;
    private final ThumbnailService thumbnailService;

    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 10;

    /**
     * 30초마다 썸네일 생성이 필요한 작업을 찾아 처리합니다.
     */
    @Scheduled(fixedRate = 30000)
    @SchedulerLock(name = "generateThumbnails", lockAtLeastFor = "PT25S", lockAtMostFor = "PT29S")
    public void generateThumbnails() {
        log.debug("Running thumbnail generation scheduler...");

        Page<Board> pendingBoards = boardRepository.findBoardsByThumbnailStatusInAndRetryCountLessThan(
            List.of(ThumbnailStatus.PENDING),
            MAX_RETRIES,
            PageRequest.of(0, BATCH_SIZE)
        );

        if (pendingBoards.isEmpty()) {
            return;
        }

        log.info("Found {} boards pending for thumbnail generation.", pendingBoards.getNumberOfElements());

        for (Board board : pendingBoards) {
            try {
                // 각 작업은 개별 트랜잭션으로 처리되도록 서비스 메소드 내에서 트랜잭션이 관리됨
                thumbnailService.generateThumbnail(board.getId());
            } catch (Exception e) {
                log.error("Error while processing thumbnail for boardId: {}", board.getId(), e);
            }
        }
    }
}
