package com.memorylab.domain.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글에 달린 댓글들을 페이징하여 조회합니다.
     * @param boardId 게시글 ID
     * @param pageable 페이징 정보
     * @return 댓글의 페이지
     */
    Page<Comment> findByBoardId(Long boardId, Pageable pageable);
}
