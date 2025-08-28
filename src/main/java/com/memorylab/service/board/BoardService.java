package com.memorylab.service.board;
import com.memorylab.domain.board.board;
import com.memorylab.repository.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class BoardService {
    private final BoardRepository repo;

    public board create(String title, String content){
        return repo.save(board.builder().title(title).content(content).build());
    }
    @Transactional(readOnly = true)
    public List<board> findAll(){ return repo.findAll(); }
}
