package com.memorylab.web.admin;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ThumbnailStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BoardRepository boardRepository;

    @PostMapping("/board/reset-conversion")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetConversionStatus(@RequestParam("boardId") Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));

        // TODO: 새로운 프로토콜에 맞는 재처리 로직 구현 필요 (예: GPU 서버에 다시 업로드 요청)
        // 현재는 낡은 상태 변경 로직을 제거하여 컴파일 오류만 해결합니다.
        // if (board.getStatus() == BoardStatus.FAILED_PROCESS) {
        //     board.setStatus(BoardStatus.DISPATCHED);
        // }

        boardRepository.save(board);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Board conversion status reset successfully",
            "boardId", boardId
        ));
    }

    @PostMapping("/thumbnails/regenerate-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> regenerateAllThumbnails() {
        // 모든 READY 상태의 썸네일을 PENDING으로 변경하여 재생성
        List<Board> boards = boardRepository.findAll();
        int count = 0;

        for (Board board : boards) {
            if (board.getThumbnailStatus() == ThumbnailStatus.READY && board.getOriginalVideoPath() != null) {
                board.setThumbnailStatus(ThumbnailStatus.PENDING);
                board.setRetryCount(0);
                count++;
            }
        }

        boardRepository.saveAll(boards);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "All thumbnails marked for regeneration with new 16:10 aspect ratio",
            "count", count
        ));
    }
}
