package com.memorylab.dto.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.Visibility;
import com.memorylab.domain.user.Member;
import lombok.Builder;

@Builder
public record BoardCreateRequestDto(
        String title,
        String content,
        Category category,
        Visibility visibility
) {
    public Board toEntity(Member user) {
        return Board.builder()
                .user(user)
                .title(title)
                .content(content)
                .category(category)
                .visibility(visibility)
                .build();
    }
}
