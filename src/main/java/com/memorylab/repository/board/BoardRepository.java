package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
    SELECT b FROM Board b
    WHERE
        (:keyword IS NULL OR b.title LIKE CONCAT('%', :keyword, '%') OR CAST(b.content as string) LIKE CONCAT('%', :keyword, '%'))
        AND (:category IS NULL OR b.category = :category)
        AND (
            b.visibility = com.memorylab.domain.board.Visibility.PUBLIC OR
            (b.visibility = com.memorylab.domain.board.Visibility.PRIVATE AND b.author.id = :meId)
        )
    """)
    Page<Board> search(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("meId") Long meId,
            Pageable pageable
    );

    Optional<Board> findById(Long id);
}

