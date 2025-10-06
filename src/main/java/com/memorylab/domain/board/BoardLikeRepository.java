package com.memorylab.domain.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface BoardLikeRepository extends JpaRepository<BoardLike, BoardLikeId> {

    /**
     * 특정 사용자가 주어진 여러 게시물 ID 중에서 '좋아요'를 누른 게시물 ID들만 조회합니다.
     * @param userId 현재 사용자 ID
     * @param boardIds 확인할 게시물 ID 목록
     * @return '좋아요'를 누른 게시물 ID의 Set
     */
    @Query("SELECT bl.id.boardId FROM BoardLike bl WHERE bl.id.userId = :userId AND bl.id.boardId IN :boardIds")
    Set<Long> findLikedBoardIdsByUserIdAndBoardIds(@Param("userId") Long userId, @Param("boardIds") List<Long> boardIds);

    /**
     * 특정 게시글의 '좋아요' 개수를 조회합니다.
     * @param boardId 게시글 ID
     * @return '좋아요' 개수
     */
    long countByBoardId(Long boardId);
}
