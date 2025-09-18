// src/main/java/com/memorylab/dto/board/BoardDtos.java
package com.memorylab.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class BoardDtos {

    // ==== 생성 요청 (tags 추가)====
    // 동영상 파일은 MultipartFile로 별도 처리되므로 DTO에는 파일 관련 정보가 포함되지 않음
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

    // ==== 목록 응답 (tags 필드 추가)====
    public record SummaryRes(
            Long id,
            String title,
            String category,
            String visibility,
            String thumbnailUrl,     // 썸네일 URL
            String conversionStatus, // 변환 상태
            String tags,             // 태그 필드 추가
            long viewCount,
            LocalDateTime createdAt,
            String authorNickname
    ) {}


    // ==== 상세 응답 (동영상 정보 추가)====
    public record DetailRes(
            Long id,
            String title,
            String content,
            String category,
            String visibility,
            String videoUrl,         // 원본 동영상 URL
            String thumbnailUrl,     // 썸네일 URL
            String tags,             // 태그
            String conversionStatus, // 변환 상태
            long viewCount,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            Long authorId,
            String authorNickname
    ) {}

}
