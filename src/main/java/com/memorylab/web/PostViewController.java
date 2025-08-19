// src/main/java/com/memorylab/web/PostViewController.java
package com.memorylab.web;

import com.memorylab.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequiredArgsConstructor
@Controller
public class PostViewController {

    private final PostService postService;

    @GetMapping("/post/list")
    public String list(Model model) {
        model.addAttribute("posts", postService.findAll()); // findAll 또는 최신 N개
        return "post/list";
    }
}
