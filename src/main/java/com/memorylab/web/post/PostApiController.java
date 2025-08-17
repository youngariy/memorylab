// PostApiController.java
package com.memorylab.web.post;

import com.memorylab.domain.post.Post;
import com.memorylab.dto.post.PostCreateRequest;
import com.memorylab.repository.post.PostRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostApiController {
    private final PostRepository repo;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid PostCreateRequest req) {
        Post saved = repo.save(Post.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .build());
        return ResponseEntity.created(URI.create("/api/posts/" + saved.getId())).body(saved);
    }

    @GetMapping
    public List<Post> list() { return repo.findAll(); }
}
