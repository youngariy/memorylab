// src/main/java/com/memorylab/service/board/BoardService.java
package com.memorylab.service.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.user.User;
import com.memorylab.dto.board.BoardDtos.*;
import com.memorylab.repository.board.BoardRepository;
import com.memorylab.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
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
        Page<Board> page;
        if (q != null && !q.isBlank()) {
            page = boards.findByTitleContainingIgnoreCase(q, pageable);
        } else if (category != null && !category.isBlank()){
            page = boards.findByCategory(category, pageable);
        } else {
            page = boards.findAll(pageable);
        }
        return page.map(b -> new SummaryRes(
                b.getId(), b.getTitle(), b.getCategory(), b.getViewCount(),
                b.getCreatedAt(), b.getAuthor().getNickname()
        ));
    }

    public DetailRes read(Long id, boolean increaseView){
        Board b = boards.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (increaseView) b.increaseView();
        return new DetailRes(
                b.getId(), b.getTitle(), b.getContent(), b.getCategory(),
                b.getViewCount(), b.getCreatedAt(), b.getUpdatedAt(),
                b.getAuthor().getId(), b.getAuthor().getNickname()
        );
    }

    public void update(Long id, Long userId, UpdateReq req){
        Board b = boards.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (!b.isAuthor(userId)) throw new SecurityException("수정 권한 없음");
        b = Board.builder()
                .id(b.getId())
                .author(b.getAuthor())
                .viewCount(b.getViewCount())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .title(req.title())
                .content(req.content())
                .category(req.category())
                .build();
        boards.save(b);
    }

    public void delete(Long id, Long userId){
        Board b = boards.findById(id).orElseThrow(() -> new IllegalArgumentException("게시글 없음"));
        if (!b.isAuthor(userId)) throw new SecurityException("삭제 권한 없음");
        boards.delete(b);
    }
}
