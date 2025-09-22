// src/main/java/com/memorylab/dto/board/BoardDtos.java
package com.memorylab.dto.board;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

// null인 필드는 응답에 포함하지 않도록 설정
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoardDtos {

    // ==== 생성 요청 (tags 추가)====
    public record CreateReq(
            @NotBlank @Size(max=120) String title,
            @NotBlank String content,
            @NotBlank String category,   // NOTICE / FREE / QNA
            String visibility,            // PUBLIC / PRIVATE (null이면 PUBLIC 처리)
            String tags     // 쉼표로 구분된 태그 문자열
    ) {}

    // ==== 수정 요청 (tags 추가)====
    public record UpdateReq(
            @NotBlank @Size(max=120) String title,
            @NotBlank String content,
            @NotBlank String category,
            String visibility,
            String tags // 태그 수정
    ) {}

    // ==== 목록 응답 (progress, errorMessage 필드 추가)====
    public record SummaryRes(
            Long id,
            String title,
            String category,
            String visibility,
            String thumbnailUrl,
            String conversionStatus,
            Integer progress, // 변환 진행률 (0-100)
            String errorMessage,
            String tags,
            long viewCount,
            LocalDateTime createdAt,
            String authorNickname
    ) {}


    // ==== 상세 응답 (originalVideoUrl 필드 제거)====
    public record DetailRes(
            Long id,
            String title,
            String content,
            String category,
            String visibility,
            // String originalVideoUrl, // 원본 URL은 외부에 노출하지 않음
            String convertedVideoUrl,
            String thumbnailUrl,
            String tags,
            String conversionStatus,
            Integer progress, // 변환 진행률 (0-100)
            String errorMessage,
            long viewCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long authorId,
            String authorNickname
    ) {}

}
