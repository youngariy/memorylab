// src/main/java/com/memorylab/dto/board/BoardDtos.java
package com.memorylab.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class BoardDtos {
    public record CreateReq(
            @NotBlank @Size(max=120) String title,
            @NotBlank String content,
            String category
    ) {}
    public record UpdateReq(
            @NotBlank @Size(max=120) String title,
            @NotBlank String content,
            String category
    ) {}
    public record SummaryRes(
            Long id, String title, String category, long viewCount,
            LocalDateTime createdAt, String authorNickname
    ) {}
    public record DetailRes(
            Long id, String title, String content, String category,
            long viewCount, LocalDateTime createdAt, LocalDateTime updatedAt,
            Long authorId, String authorNickname
    ){}
}
