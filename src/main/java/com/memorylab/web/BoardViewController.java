// src/main/java/com/memorylab/web/PostViewController.java
package com.memorylab.web;

import com.memorylab.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequiredArgsConstructor
@Controller
public class BoardViewController {

    private final BoardService boardService;

    @GetMapping("/board/list")
    public String list(Model model) {
        model.addAttribute("boards", boardService.findAll()); // findAll 또는 최신 N개
        return "board/list";
    }
}
