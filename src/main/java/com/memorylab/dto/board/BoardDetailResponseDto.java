package com.memorylab.dto.board;

import com.memorylab.domain.BoardStatus;
import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ThumbnailStatus;
import com.memorylab.domain.board.TranscodeStatus;
import com.memorylab.domain.board.Visibility;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record BoardDetailResponseDto(
    Long id,
    String title,
    String content,
    Category category,
    Visibility visibility,
    int viewCount,
    int likeCount,
    int commentCount,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt,
    Long authorId,
    String authorNickname,
    boolean isLikedByCurrentUser,

    // AI 모델 관련 필드
    String aiTaskId,
    String plyPath,
    String gpuErrorMessage,

    // 썸네일 관련 필드
    String thumbnailPath,
    ThumbnailStatus thumbnailStatus,

    // 동영상 변환 관련 필드
    String convertedVideoPath,
    TranscodeStatus transcodeStatus,

    // 동영상 유무
    boolean hasVideo,

    // 최종 계산된 상태
    BoardStatus status
) {
    // 수정: BoardStatus를 외부에서 받아오도록 시그니처 변경
    public static BoardDetailResponseDto fromEntity(Board board, boolean isLiked, BoardStatus calculatedStatus) {
        return BoardDetailResponseDto.builder()
            .id(board.getId())
            .title(board.getTitle())
            .content(board.getContent())
            .category(board.getCategory())
            .visibility(board.getVisibility())
            .viewCount(board.getViewCount())
            .likeCount(board.getLikeCount())
            .commentCount(board.getCommentCount())
            .createdAt(board.getCreatedAt())
            .modifiedAt(board.getModifiedAt())
            .authorId(board.getUser().getId())
            .authorNickname(board.getUser().getNickname())
            .isLikedByCurrentUser(isLiked)
            
            // 소스 오브 트루스 필드들
            .convertedVideoPath(board.getConvertedVideoPath())
            .thumbnailPath(board.getThumbnailPath())
            .thumbnailStatus(board.getThumbnailStatus())
            .transcodeStatus(board.getTranscodeStatus())

            // AI 모델 관련 필드
            .aiTaskId(board.getAiTaskId())
            .plyPath(board.getPlyPath())
            .gpuErrorMessage(board.getGpuErrorMessage())

            // 동영상 유무
            .hasVideo(board.getOriginalVideoPath() != null && !board.getOriginalVideoPath().isBlank())

            // 최종 계산된 상태 주입
            .status(calculatedStatus)
            .build();
    }
}
