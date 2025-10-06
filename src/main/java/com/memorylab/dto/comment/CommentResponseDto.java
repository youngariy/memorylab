package com.memorylab.dto.comment;

import com.memorylab.domain.comment.Comment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CommentResponseDto(
        Long id,
        String content,
        String authorNickname,
        Long authorId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {
    public static CommentResponseDto fromEntity(Comment comment) {
        return CommentResponseDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .authorNickname(comment.getUser() != null ? comment.getUser().getNickname() : "(알 수 없음)")
                .authorId(comment.getUser() != null ? comment.getUser().getId() : null)
                .createdAt(comment.getCreatedAt())
                .modifiedAt(comment.getModifiedAt())
                .build();
    }
}
