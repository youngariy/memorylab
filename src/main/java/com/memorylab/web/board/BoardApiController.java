// PostApiController.java
package com.memorylab.web.board;

import com.memorylab.domain.board.board;
import com.memorylab.dto.board.BoardCreateRequest;
import com.memorylab.repository.board.BoardRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardApiController {
    private final BoardRepository repo;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid BoardCreateRequest req) {
        board saved = repo.save(board.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .build());
        return ResponseEntity.created(URI.create("/api/boards/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<board> list() { return repo.findAll(); }
}
