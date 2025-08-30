// src/main/java/com/memorylab/repository/board/projection/BoardDetailProj.java
package com.memorylab.repository.board.projection;

import java.time.LocalDateTime;

public interface BoardDetailProj {
    Long getId();
    String getTitle();
    String getContent();
    String getCategory();
    long getViewCount();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    AuthorProj getAuthor(); // 중첩 프로젝션
}
