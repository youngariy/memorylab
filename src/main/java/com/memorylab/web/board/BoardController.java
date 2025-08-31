// src/main/java/com/memorylab/web/board/BoardController.java
package com.memorylab.web.board;

import com.memorylab.dto.board.BoardDtos.CreateReq;
import com.memorylab.dto.board.BoardDtos.UpdateReq;
import com.memorylab.dto.board.BoardDtos.SummaryRes;
import com.memorylab.dto.board.BoardDtos.DetailRes;
import com.memorylab.service.board.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService board;

    /** 로그인 강제 헬퍼 */
    private Long requireUserId(Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userId;
    }

    /** 글 생성 */
    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) Long userId,
            @RequestBody @Valid CreateReq req
    ) {
        Long id = board.create(requireUserId(userId), req);
        return ResponseEntity.ok(java.util.Map.of("id", id));
    }

    /** 목록: PUBLIC + 내 PRIVATE만 노출 (검색/카테고리/페이지네이션) */
    @GetMapping
    public ResponseEntity<Page<SummaryRes>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) Long meId
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")));
        return ResponseEntity.ok(board.list(q, category, meId, pageable));
    }

    /** 상세: 가시성 체크 + (기본) 조회수 증가 */
    @GetMapping("/{id}")
    public ResponseEntity<DetailRes> detail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "true") boolean increaseView,
            @AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) Long meId
    ) {
        return ResponseEntity.ok(board.read(id, meId, increaseView));
    }

    /** 수정: 작성자만 */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) Long userId,
            @RequestBody @Valid UpdateReq req
    ) {
        board.update(id, requireUserId(userId), req);
        return ResponseEntity.ok().build();
    }

    /** 삭제: 작성자만 */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal(expression = "id", errorOnInvalidType = false) Long userId
    ) {
        board.delete(id, requireUserId(userId));
        return ResponseEntity.ok().build();
    }
}
