package com.memorylab.service.post;
import com.memorylab.domain.post.Post;
import com.memorylab.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class PostService {
    private final PostRepository repo;

    public Post create(String title, String content){
        return repo.save(Post.builder().title(title).content(content).build());
    }
    @Transactional(readOnly = true)
    public List<Post> findAll(){ return repo.findAll(); }
}
