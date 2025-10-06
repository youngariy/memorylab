package com.memorylab.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CommentUpdateRequestDto(
        @NotBlank(message = "댓글 내용은 비워둘 수 없습니다.")
        String content
) {
}
