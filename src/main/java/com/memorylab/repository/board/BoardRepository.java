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
    select b
    from Board b
      join b.author a
    where (:category is null or b.category = :category)
      and (
            :q is null or :q = '' or
            lower(b.title) like lower(concat('%', :q, '%')) or
            lower(cast(b.content as string)) like lower(concat('%', :q, '%')) or
            lower(a.nickname) like lower(concat('%', :q, '%'))
          )
      and (
            b.visibility = com.memorylab.domain.board.Visibility.PUBLIC
            or (:meId is not null and a.id = :meId)
          )
    """)
    Page<Board> searchVisible(@Param("q") String q,
                              @Param("category") Category category, // 아직 엔티티가 String category면 타입을 String으로 바꿔!
                              @Param("meId") Long meId,
                              Pageable pageable);


    Optional<Board> findById(Long id);
}
