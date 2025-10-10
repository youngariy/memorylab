package com.memorylab.domain.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Override
    @EntityGraph(attributePaths = {"user"})
    Page<Board> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Board> findByUser_Id(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Page<Board> findByCategory(Category category, Pageable pageable);

    // Visibility-aware queries
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT b FROM Board b WHERE b.visibility = 'PUBLIC' OR b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<Board> findAllVisibleToUser(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT b FROM Board b WHERE b.category = :category AND (b.visibility = 'PUBLIC' OR b.user.id = :userId) ORDER BY b.createdAt DESC")
    Page<Board> findByCategoryVisibleToUser(@Param("category") Category category, @Param("userId") Long userId, Pageable pageable);

    Optional<Board> findByAiTaskId(String aiTaskId);

    // --- 스케줄러용 메소드 (복원) ---
    Page<Board> findBoardsByThumbnailStatusInAndRetryCountLessThan(List<ThumbnailStatus> statuses, int retryCount, Pageable pageable);

    Page<Board> findBoardsByTranscodeStatusAndRetryCountLessThan(TranscodeStatus status, int retryCount, Pageable pageable);

    @Query("select b from Board b join fetch b.user where b.id = :id")
    Optional<Board> findByIdWithUser(@Param("id") Long id);

    List<Board> findByExternalStatusIn(List<ExternalStatus> statuses);
}
