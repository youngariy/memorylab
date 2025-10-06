package com.memorylab.service;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ThumbnailStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final BoardRepository boardRepository;

    // 업로드/서빙 경로 상수 (필요하면 @Value 로 바꿔도 됨)
    private static final Path THUMB_ROOT = Paths.get("/home/ec2-user/app/data/thumbnails");

    @Async("thumbnailExecutor") // 이미 스레드풀 설정돼 있으면 사용, 없으면 @Async 제거해도 동작
    @Transactional
    public void generateThumbnailAsync(Long boardId) {
        boardRepository.findById(boardId).ifPresent(board -> {
            if (board.getOriginalVideoPath() == null) {
                log.warn("[thumb] boardId={} has no originalVideoPath", boardId);
                markFailed(board, "NO_INPUT");
                return;
            }
            try {
                Files.createDirectories(THUMB_ROOT);
                Path input = Paths.get(board.getOriginalVideoPath());
                if (!Files.exists(input)) {
                    markFailed(board, "INPUT_NOT_FOUND");
                    log.error("[thumb] input not found: {}", input);
                    return;
                }

                // 출력 파일 (tmp -> final 원자적 갱신)
                Path outTmp  = THUMB_ROOT.resolve(boardId + ".tmp.jpg");
                Path outFile = THUMB_ROOT.resolve(boardId + ".jpg");

                // 추출 위치(초) 결정: 중간 프레임(or 최소 1초)
                int ss = pickSecond(input);

                // ffmpeg 실행 (단일 프레임 추출)
                List<String> cmd = List.of(
                    "ffmpeg", "-y",
                    "-ss", String.valueOf(ss),
                    "-i", input.toString(),
                    "-frames:v", "1",
                    "-vf", "thumbnail,scale=640:-1",
                    outTmp.toString()
                );
                int code = runAndLog(cmd, boardId);
                if (code != 0 || !Files.exists(outTmp)) {
                    markFailed(board, "FFMPEG_EXIT_" + code);
                    return;
                }

                // tmp -> final
                try {
                    Files.move(outTmp, outFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(outTmp, outFile, StandardCopyOption.REPLACE_EXISTING);
                }

                // DB 업데이트
                board.setThumbnailStatus(ThumbnailStatus.READY);
                board.setThumbnailPath("/thumbnails/" + boardId + ".jpg");
                board.setRetryCount(0);
                boardRepository.save(board);

                log.info("[thumb] DONE boardId={} -> {}", boardId, outFile);
            } catch (Exception e) {
                log.error("[thumb] FAILED boardId={} : {}", boardId, e.getMessage(), e);
                markFailed(board, "EXCEPTION");
            }
        });
    }

    // 업로드 직후 즉시 시도 (createBoard에서 호출)
    @Transactional
    public void attemptImmediateGeneration(Board board) {
        if (board.getOriginalVideoPath() == null) return;
        board.setThumbnailStatus(ThumbnailStatus.PENDING);
        board.setThumbnailPath(null);
        board.setRetryCount(0);
        boardRepository.save(board);
        generateThumbnailAsync(board.getId());
    }

    private void markFailed(Board board, String reason) {
        board.setThumbnailStatus(ThumbnailStatus.FAILED);
        board.setRetryCount(board.getRetryCount() + 1);
        boardRepository.save(board);
        log.error("[thumb] FAIL boardId={} reason={}", board.getId(), reason);
    }

    /** ffprobe로 길이 읽고, 중간 지점(최대 60초)을 선택 */
    private int pickSecond(Path input) {
        try {
            List<String> cmd = List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=nw=1:nk=1",
                input.toString()
            );
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            int code = p.waitFor();
            if (code == 0) {
                double d = Double.parseDouble(out);
                if (Double.isFinite(d) && d > 3) return (int)Math.min(Math.round(d / 2.0), 60);
            }
        } catch (Exception ignore) {}
        return 1;
    }

    /** ffmpeg/ffprobe 실행 로그 남기기 */
    private int runAndLog(List<String> cmd, Long boardId) throws IOException, InterruptedException {
        log.debug("[ffmpeg boardId={}] CMD: {}", boardId, String.join(" ", cmd));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        try (var is = p.getInputStream()) {
            is.transferTo(new java.io.OutputStream() {
                @Override public void write(int b) {}
            }); // 출력이 커지면 필요시 로그로 누적
        }
        int code = p.waitFor();
        log.debug("[ffmpeg boardId={}] exit={}", boardId, code);
        return code;
    }
}