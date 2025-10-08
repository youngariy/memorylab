package com.memorylab.dto.board;

import com.memorylab.domain.BoardStatus;
import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ThumbnailStatus;
import com.memorylab.dto.user.AuthorDto;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BoardListResponseDto(
    Long id,
    String title,
    AuthorDto author,
    Category category,
    int viewCount,
    int likeCount,
    int commentCount,
    LocalDateTime createdAt,
    boolean isLikedByCurrentUser,
    
    // 썸네일 정보 (복원)
    String thumbnailPath,
    ThumbnailStatus thumbnailStatus,

    // 최종 계산된 상태
    BoardStatus status
) {
    public static BoardListResponseDto fromEntity(Board board, boolean isLiked, BoardStatus calculatedStatus) {
        return BoardListResponseDto.builder()
            .id(board.getId())
            .title(board.getTitle())
            .author(AuthorDto.fromEntity(board.getUser()))
            .category(board.getCategory())
            .viewCount(board.getViewCount())
            .likeCount(board.getLikeCount())
            .commentCount(board.getCommentCount())
            .createdAt(board.getCreatedAt())
            .isLikedByCurrentUser(isLiked)
            
            // 썸네일 정보 (복원)
            .thumbnailPath(board.getThumbnailPath())
            .thumbnailStatus(board.getThumbnailStatus())

            // 최종 계산된 상태 주입
            .status(calculatedStatus)
            .build();
    }
}
