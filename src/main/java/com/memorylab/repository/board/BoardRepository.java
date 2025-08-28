// src/main/java/com/memorylab/repository/board/BoardRepository.java
package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, Long> {
    Page<Board> findByTitleContainingIgnoreCase(String q, Pageable pageable);
    Page<Board> findByCategory(String category, Pageable pageable);
}
