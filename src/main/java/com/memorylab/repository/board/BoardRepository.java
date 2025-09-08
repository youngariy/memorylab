// src/main/java/com/memorylab/repository/board/BoardRepository.java
package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BoardRepository extends JpaRepository<Board, Long> {

    @Query("""
        select b
        from Board b
        where (:keyword is null
                  or b.title   like concat('%', :keyword, '%')
                  or b.content like concat('%', :keyword, '%'))
          and (:category is null or b.category = :category)
          and (:authorId is null or b.author.id = :authorId)
          and (
                b.visibility = com.memorylab.domain.board.Visibility.PUBLIC
             or (:meId is not null and b.author.id = :meId)
          )
        """)
    Page<Board> search(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("authorId") Long authorId,
            @Param("meId") Long meId,
            Pageable pageable
    );
}
