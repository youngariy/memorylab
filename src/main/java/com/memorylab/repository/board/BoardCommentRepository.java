// src/main/java/com/memorylab/repository/board/BoardCommentRepository.java
package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardComment;
import com.memorylab.repository.board.projection.AuthorProj;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardComment, Long> {

    // 엔티티 그대로
    List<BoardComment> findByBoardOrderByCreatedAtAsc(Board board);

    // 화면용(가벼운) 프로젝션
    List<CommentView> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    interface CommentView {
        Long getId();
        String getContent();
        LocalDateTime getCreatedAt();
        AuthorProj getAuthor(); // 닉네임/ID는 여기서 꺼내 쓰면 됨
    }
}
