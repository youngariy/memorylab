// src/main/java/com/memorylab/web/board/BoardController.java
package com.memorylab.web.board;

import com.memorylab.dto.board.BoardDtos.*;
import com.memorylab.service.board.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService board;

    private Long currentUserId(){
        return 1L; // TODO: JWT 연동
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid CreateReq req){
        Long id = board.create(currentUserId(), req);
        return ResponseEntity.ok(java.util.Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) String category,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="10") int size
    ){
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
        return ResponseEntity.ok(board.list(q, category, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id){
        return ResponseEntity.ok(board.read(id, true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody @Valid UpdateReq req){
        board.update(id, currentUserId(), req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id){
        board.delete(id, currentUserId());
        return ResponseEntity.ok().build();
    }
}
