package com.memorylab.domain.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Board> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Board> findByUser_Id(Long userId, Pageable pageable);

    Page<Board> findBoardsByThumbnailStatusInAndRetryCountLessThan(List<ThumbnailStatus> statuses, int retryCount, Pageable pageable);

    Page<Board> findBoardsByTranscodeStatusAndRetryCountLessThan(TranscodeStatus status, int retryCount, Pageable pageable);
}
