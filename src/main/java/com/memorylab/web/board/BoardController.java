// src/main/java/com/memorylab/web/board/BoardController.java
package com.memorylab.web.board;

import com.memorylab.dto.board.BoardDtos.CreateReq;
import com.memorylab.dto.board.BoardDtos.UpdateReq;
import com.memorylab.dto.board.BoardDtos.SummaryRes;
import com.memorylab.dto.board.BoardDtos.DetailRes;
import com.memorylab.service.board.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService board; // ✅ 컨트롤러는 서비스만 의존

    private Long requireUserId(@AuthenticationPrincipal(expression = "id") Long userId) {
        if (userId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return userId;
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal(expression = "id") Long userId,
                                    @RequestBody @Valid CreateReq req) {
        Long id = board.create(requireUserId(userId), req);
        return ResponseEntity.ok(java.util.Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<Page<SummaryRes>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        return ResponseEntity.ok(board.list(q, category, pageable)); // ✅ 서비스로 일원화
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailRes> detail(@PathVariable Long id) {
        return ResponseEntity.ok(board.read(id, true)); // ✅ 서비스로 일원화
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @AuthenticationPrincipal(expression = "id") Long userId,
                                    @RequestBody @Valid UpdateReq req) {
        board.update(id, requireUserId(userId), req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal(expression = "id") Long userId) {
        board.delete(id, requireUserId(userId));
        return ResponseEntity.ok().build();
    }
}
