package com.memorylab.dto.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ThumbnailStatus;
import com.memorylab.domain.board.TranscodeStatus;
import lombok.Builder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Builder
public record BoardListResponseDto(
        Long id,
        String title,
        String authorNickname,
        Category category,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isLikedByCurrentUser,
        LocalDateTime createdAt,
        String thumbnailUrl,
        ThumbnailStatus thumbnailStatus, // 썸네일 상태 추가
        TranscodeStatus transcodeStatus, // 트랜스코딩 상태 추가
        boolean isNotice
) {
    public static BoardListResponseDto fromEntity(Board board, boolean isLiked) {
        String thumbnailUrl = board.getThumbnailPath();
        if (thumbnailUrl != null && board.getThumbnailStatus() == ThumbnailStatus.READY) {
            thumbnailUrl = withCacheBuster(thumbnailUrl);
        }

        return BoardListResponseDto.builder()
                .id(board.getId())
                .title(board.getTitle())
                .authorNickname(board.getUser() != null ? board.getUser().getNickname() : "(알 수 없음)")
                .category(board.getCategory())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .isLikedByCurrentUser(isLiked)
                .createdAt(board.getCreatedAt())
                .thumbnailUrl(thumbnailUrl)
                .thumbnailStatus(board.getThumbnailStatus())
                .transcodeStatus(board.getTranscodeStatus())
                .isNotice(board.getCategory() == Category.NOTICE)
                .build();
    }

    private static String withCacheBuster(String url) {
        if (url == null) return null;
        try {
            if (url.startsWith("/thumbnails/")) {
                Path p = Paths.get("/home/ec2-user/app/data" + url);
                if (Files.exists(p)) {
                    long v = Files.getLastModifiedTime(p).toMillis();
                    return url + "?v=" + v;
                }
            }
        } catch (IOException ignored) {}
        return url;
    }
}
