// src/main/java/com/memorylab/web/board/BoardPageController.java
package com.memorylab.web.board;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BoardPageController {
    @GetMapping("/board/list")  public String list() { return "board/list"; }
    @GetMapping("/board/view")  public String view() { return "board/view"; }
    @GetMapping("/board/new")   public String create() { return "board/new"; }
    @GetMapping("/board/edit")  public String edit() { return "board/edit"; }
}
