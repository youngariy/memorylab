package com.memorylab.repository.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ConversionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    @Query(""" 
        select b from Board b join fetch b.author
        where (:status is null or b.conversionStatus = :status)
          and (:keyword is null or b.title like concat('%', :keyword, '%') or b.author.nickname like concat('%', :keyword, '%'))
          and (:from is null or b.createdAt >= :from)
          and (:to is null or b.createdAt <= :to)
        """)
    Page<Board> searchForAdmin(
            @Param("status") ConversionStatus status,
            @Param("keyword") String keyword,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    List<Board> findByOriginalVideoPathIsNotNullAndConversionStatusIsNull();

    Page<Board> findByConversionStatusOrderByCreatedAtAsc(ConversionStatus status, Pageable pageable);

    /**
     * 특정 상태(예: PROCESSING)에 있으면서, 특정 시간 이전에 마지막으로 업데이트된 작업을 조회합니다.
     * 콜백이 유실되었을 가능성이 있는 오래된 작업을 찾기 위해 폴링 스케줄러가 사용합니다.
     *
     * @param status    찾고자 하는 변환 상태
     * @param threshold 이 시간 이전에 업데이트된 작업을 찾습니다.
     * @param pageable  조회할 개수
     * @return 조건을 만족하는 Board 엔티티의 Page
     */
    Page<Board> findByConversionStatusAndUpdatedAtBefore(ConversionStatus status, LocalDateTime threshold, Pageable pageable);
}
