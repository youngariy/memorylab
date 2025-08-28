// PostRepository.java
package com.memorylab.repository.board;
import com.memorylab.domain.board.board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<board, Long> {}
