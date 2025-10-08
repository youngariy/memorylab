package com.memorylab.dto.board;

import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
public record BoardPageResponseDto(
    List<BoardListResponseDto> content,
    int totalPages,
    long totalElements,
    int currentPage,
    int pageSize,
    boolean isFirst,
    boolean isLast
) {
    public static BoardPageResponseDto fromPage(Page<BoardListResponseDto> page) {
        return BoardPageResponseDto.builder()
            .content(page.getContent())
            .totalPages(page.getTotalPages())
            .totalElements(page.getTotalElements())
            .currentPage(page.getNumber())
            .pageSize(page.getSize())
            .isFirst(page.isFirst())
            .isLast(page.isLast())
            .build();
    }
}
