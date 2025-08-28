// src/main/java/com/memorylab/repository/board/BoardCommentRepository.java
package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {
    List<BoardComment> findByBoardOrderByCreatedAtAsc(Board board);
}
