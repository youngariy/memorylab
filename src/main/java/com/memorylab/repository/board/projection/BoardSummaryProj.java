// src/main/java/com/memorylab/repository/board/projection/BoardSummaryProj.java
package com.memorylab.repository.board.projection;

import java.time.LocalDateTime;

public interface BoardSummaryProj {
    Long getId();
    String getTitle();
    String getCategory();

    AuthorProj getAuthor(); // 중첩 프로젝션
}
