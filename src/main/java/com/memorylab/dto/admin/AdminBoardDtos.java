package com.memorylab.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

// null인 필드는 응답에 포함하지 않도록 설정
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminBoardDtos {

    /**
     * 관리자용 게시글 목록 조회를 위한 요약 응답 DTO
     */
    public record AdminBoardSummaryRes(
            Long id,
            String title,
            String authorNickname,
            String status,
            Integer progress,
            String errorMessage,
            LocalDateTime createdAt
    ) {}
}
