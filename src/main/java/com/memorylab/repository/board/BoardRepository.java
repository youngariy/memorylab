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
          and (:tag is null or b.tags like concat('%', :tag, '%'))
          and (:authorId is null or b.author.id = :authorId)
          and (
                b.visibility = com.memorylab.domain.board.Visibility.PUBLIC
             or (:meId is not null and b.author.id = :meId)
             or (:isAdmin = true)
          )
        order by case when b.category = com.memorylab.domain.board.Category.NOTICE then 0 else 1 end, b.id desc
        """)
    Page<Board> search(
            @Param("keyword") String keyword,
            @Param("category") Category category,
            @Param("tag") String tag,
            @Param("authorId") Long authorId,
            @Param("meId") Long meId,
            @Param("isAdmin") boolean isAdmin,
            Pageable pageable
    );
}
