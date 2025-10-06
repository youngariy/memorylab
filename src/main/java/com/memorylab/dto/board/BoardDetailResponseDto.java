package com.memorylab.dto.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ThumbnailStatus;
import com.memorylab.domain.board.TranscodeStatus;
import com.memorylab.domain.board.Visibility;
import lombok.Builder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Builder
public record BoardDetailResponseDto(
        Long id,
        String title,
        String content,
        String authorNickname,
        Long authorId,
        Category category,
        Visibility visibility,
        int viewCount,
        int likeCount,
        int commentCount,
        boolean isLikedByCurrentUser,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt,
        String thumbnailUrl,
        ThumbnailStatus thumbnailStatus,
        TranscodeStatus transcodeStatus,
        String convertedVideoPath
) {
    public static BoardDetailResponseDto fromEntity(Board board, boolean isLiked) {
        String thumbnailUrl = board.getThumbnailPath();
        if (thumbnailUrl != null && board.getThumbnailStatus() == ThumbnailStatus.READY) {
            thumbnailUrl = withCacheBuster(thumbnailUrl);
        }

        return BoardDetailResponseDto.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .authorNickname(board.getUser() != null ? board.getUser().getNickname() : "(알 수 없음)")
                .authorId(board.getUser() != null ? board.getUser().getId() : null)
                .category(board.getCategory())
                .visibility(board.getVisibility())
                .viewCount(board.getViewCount())
                .likeCount(board.getLikeCount())
                .commentCount(board.getCommentCount())
                .isLikedByCurrentUser(isLiked)
                .createdAt(board.getCreatedAt())
                .modifiedAt(board.getModifiedAt())
                .thumbnailUrl(thumbnailUrl)
                .thumbnailStatus(board.getThumbnailStatus())
                .transcodeStatus(board.getTranscodeStatus())
                .convertedVideoPath(board.getConvertedVideoPath())
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
