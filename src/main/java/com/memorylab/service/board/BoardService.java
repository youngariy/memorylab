// src/main/java/com/memorylab/service/board/BoardService.java
package com.memorylab.service.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.Visibility;
import com.memorylab.domain.user.User;
import com.memorylab.dto.board.BoardDtos.CreateReq;
import com.memorylab.dto.board.BoardDtos.UpdateReq;
import com.memorylab.dto.board.BoardDtos.SummaryRes;
import com.memorylab.dto.board.BoardDtos.DetailRes;
import com.memorylab.repository.board.BoardRepository;
import com.memorylab.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boards;
    private final UserRepository users;

    public Long create(Long userId, CreateReq req){
        User author = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유저 없음"));

        Board b = Board.builder()
                .author(author)
                .title(nz(req.title()))
                .content(nz(req.content()))
                .category(Category.parse(req.category()))
                .visibility(Visibility.parse(req.visibility()))
                .build();

        return boards.save(b).getId();
    }

    @Transactional(readOnly = true)
    public Page<SummaryRes> list(String q, String category, Long authorId, Long meId, Pageable pageable){
        Category cat = Category.parse(category);
        Page<Board> page = boards.search(q, cat, authorId, meId, pageable);

        return page.map(b -> new SummaryRes(
                b.getId(),
                b.getTitle(),
                b.getCategory() == null ? null : b.getCategory().name(),
                b.getVisibility() == null ? Visibility.PUBLIC.name() : b.getVisibility().name(),
                b.getViewCount(),
                b.getCreatedAt(),
                b.getAuthor() == null ? null : b.getAuthor().getNickname()
        ));
    }

    @Transactional(readOnly = false)
    public DetailRes read(Long id, Long meId, boolean increaseView){
        Board b = boards.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));

        if (b.getVisibility() == Visibility.PRIVATE) {
            if (meId == null || !b.isAuthor(meId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 글");
            }
        }

        if (increaseView) b.increaseView();

        return new DetailRes(
                b.getId(),
                b.getTitle(),
                b.getContent(),
                b.getCategory() == null ? null : b.getCategory().name(),
                b.getVisibility() == null ? Visibility.PUBLIC.name() : b.getVisibility().name(),
                b.getViewCount(),
                b.getCreatedAt(),
                b.getUpdatedAt(),
                b.getAuthor() == null ? null : b.getAuthor().getId(),
                b.getAuthor() == null ? null : b.getAuthor().getNickname()
        );
    }

    public void update(Long id, Long userId, UpdateReq req){
        Board b = boards.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (!b.isAuthor(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한 없음");

        b.modify(
                nz(req.title()),
                nz(req.content()),
                Category.parse(req.category()),
                Visibility.parse(req.visibility())
        );
    }

    public void delete(Long id, Long userId){
        Board b = boards.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (!b.isAuthor(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한 없음");
        boards.delete(b);
    }

    private String nz(String s){ return (s == null) ? "" : s.trim(); }
}
