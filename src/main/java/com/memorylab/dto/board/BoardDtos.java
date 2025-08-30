// src/main/java/com/memorylab/dto/board/BoardDtos.java
package com.memorylab.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class BoardDtos {

    // ==== 생성 요청 ====
    public record CreateReq(
            @NotBlank @Size(max=120) String title,
            @NotBlank String content,
            @NotBlank String category,   // NOTICE / FREE / QNA
            String visibility            // PUBLIC / PRIVATE (null이면 PUBLIC 처리)
    ) {}

    // ==== 수정 요청 ====
    public record UpdateReq(
            @NotBlank @Size(max=120) String title,
            @NotBlank String content,
            @NotBlank String category,
            String visibility
    ) {}

    // ==== 목록 응답 ====
    public record SummaryRes(
            Long id,
            String title,
            String category,
            String visibility,   // ★ 추가
            long viewCount,
            LocalDateTime createdAt,
            String authorNickname
    ) {}


    // ==== 상세 응답 ====
    public record DetailRes(
            Long id,
            String title,
            String content,
            String category,
            String visibility,
            long viewCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long authorId,
            String authorNickname
    ) {}

}
