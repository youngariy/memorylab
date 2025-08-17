// PostRepository.java
package com.memorylab.repository.post;
import com.memorylab.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {}
