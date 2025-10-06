package com.memorylab.dto.board;

import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.Visibility;

public record BoardUpdateRequestDto(
        String title,
        String content,
        Category category,
        Visibility visibility
) {
}
