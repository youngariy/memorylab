package com.memorylab.service;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ThumbnailStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final BoardRepository boardRepository;
    private final FileService fileService; // LocalFileService가 주입될 것임

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Transactional
    public void generateThumbnail(Long boardId) {
        Board board = boardRepository.findById(boardId)
            .orElseThrow(() -> new IllegalArgumentException("Board not found: " + boardId));

        if (board.getOriginalVideoPath() == null) {
            log.warn("Original video path is null for boardId: {}. Cannot generate thumbnail.", boardId);
            board.setThumbnailStatus(ThumbnailStatus.FAILED);
            return;
        }

        Path videoPath = Paths.get(board.getOriginalVideoPath());
        // 썸네일 경로는 FileService가 아닌, 정해진 규칙에 따라 생성
        Path thumbnailPath = Paths.get(fileService.getUploadRootDir().toString(), "thumbnails", boardId + ".jpg");

        try {
            Files.createDirectories(thumbnailPath.getParent());

            ProcessBuilder processBuilder = new ProcessBuilder(
                ffmpegPath,
                "-i", videoPath.toString(),
                "-ss", "00:00:01.000", // 1초 시점의 프레임
                "-vframes", "1",
                "-q:v", "2", // 높은 품질
                thumbnailPath.toString(),
                "-y" // 덮어쓰기 허용
            );

            log.info("Executing ffmpeg command for boardId: {}: {}", boardId, String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();

            // ffmpeg 프로세스의 출력을 로깅 (디버깅용)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[ffmpeg] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Thumbnail generated successfully for boardId: {}", boardId);
                board.setThumbnailStatus(ThumbnailStatus.READY);
                board.setThumbnailPath("/thumbnails/" + boardId + ".jpg"); // 웹 접근 경로
            } else {
                throw new IOException("ffmpeg process exited with code: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Thumbnail generation failed for boardId: {}", boardId, e);
            board.setThumbnailStatus(ThumbnailStatus.FAILED);
            board.increaseRetryCount();
        }
    }
}
