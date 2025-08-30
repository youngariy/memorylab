// src/main/java/com/memorylab/service/board/BoardService.java
package com.memorylab.service.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.user.User;
import com.memorylab.dto.board.BoardDtos.*;
import com.memorylab.repository.board.BoardRepository;
import com.memorylab.repository.board.projection.BoardSummaryProj;
import com.memorylab.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boards;
    private final UserRepository users;

    public Long create(Long userId, CreateReq req){
        User author = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저 없음"));
        Board b = Board.builder()
                .author(author)
                .title(req.title())
                .content(req.content())
                .category(req.category())
                .build();
        return boards.save(b).getId();
    }

    @Transactional(readOnly = true)
    public Page<SummaryRes> list(String q, String category, Pageable pageable){
        Page<BoardSummaryProj> page;
        if (q != null && !q.isBlank()) page = boards.findByTitleContainingIgnoreCase(q, pageable);
        else if (category != null && !category.isBlank()) page = boards.findByCategory(category, pageable);
        else page = boards.findAllBy(pageable);

        return page.map(p -> new SummaryRes(
                p.getId(),
                p.getTitle(),
                p.getCategory(),
                p.getViewCount(),
                p.getCreatedAt(),
                p.getAuthor() != null ? p.getAuthor().getNickname() : null
        ));
    }

    @Transactional(readOnly = true)
    public DetailRes read(Long id, boolean increaseView){
        // 조회수 증가는 엔티티로 한 번 로드해서 처리
        if (increaseView) {
            boards.findById(id).ifPresent(Board::increaseView);
        }

        return boards.findDetailById(id)
                .map(p -> new DetailRes(
                        p.getId(),
                        p.getTitle(),
                        p.getContent(),
                        p.getCategory(),
                        p.getViewCount(),
                        p.getCreatedAt(),
                        p.getUpdatedAt(),
                        p.getAuthor() != null ? p.getAuthor().getId() : null,
                        p.getAuthor() != null ? p.getAuthor().getNickname() : null
                ))
                .orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
    }

    public void update(Long id, Long userId, UpdateReq req){
        Board b = boards.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (!b.isAuthor(userId)) throw new SecurityException("수정 권한 없음");

        b.modify(req.title(), req.content(), req.category());

    }

    public void delete(Long id, Long userId){
        Board b = boards.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (!b.isAuthor(userId)) throw new SecurityException("삭제 권한 없음");
        boards.delete(b);
    }
}
