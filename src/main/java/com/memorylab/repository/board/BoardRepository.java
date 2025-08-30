// src/main/java/com/memorylab/repository/board/BoardRepository.java
package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import com.memorylab.repository.board.projection.BoardDetailProj;
import com.memorylab.repository.board.projection.BoardSummaryProj;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    // 목록용(검색)
    Page<BoardSummaryProj> findByTitleContainingIgnoreCase(String q, Pageable pageable);

    // 목록용(카테고리)

    Page<BoardSummaryProj> findByCategory(String category, Pageable pageable);

    // 목록용(전체) — 조건 없이 프로젝션으로 페이지네이션
    Page<BoardSummaryProj> findAllBy(Pageable pageable);

    // 상세보기용S
    Optional<BoardDetailProj> findDetailById(Long id);
}
