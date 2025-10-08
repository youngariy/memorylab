package com.memorylab.dto.user;

import com.memorylab.domain.user.Member;
import lombok.Builder;

@Builder
public record AuthorDto(
    Long id,
    String nickname
) {
    public static AuthorDto fromEntity(Member member) {
        return AuthorDto.builder()
            .id(member.getId())
            .nickname(member.getNickname())
            .build();
    }
}
