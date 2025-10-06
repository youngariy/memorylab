package com.memorylab.web.admin;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.TranscodeStatus;
import com.memorylab.dto.board.BoardListResponseDto;
import com.memorylab.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final BoardService boardService;
    private final BoardRepository boardRepository; // 직접 접근을 위해 추가

    // 관리자용 게시판 목록 조회 (기존 BoardListResponseDto 재활용)
    @GetMapping("/boards")
    public ResponseEntity<Page<BoardListResponseDto>> getBoardListForAdmin(Pageable pageable) {
        // 현재는 userId를 null로 넘겨 좋아요 상태 없이 모든 글을 조회
        Page<BoardListResponseDto> list = boardService.getBoardList(pageable, null);
        return ResponseEntity.ok(list);
    }

    // 동영상 변환 재시도 API
    @PostMapping("/boards/{id}/retry-transcode")
    @Transactional
    public ResponseEntity<?> retryTranscode(@PathVariable Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID: " + id));

        if (board.getTranscodeStatus() == TranscodeStatus.FAILED) {
            board.setTranscodeStatus(TranscodeStatus.PENDING);
            board.setRetryCount(0); // 재시도 횟수 초기화
            boardRepository.save(board);
            return ResponseEntity.ok(Map.of("status", "Transcoding job re-queued."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Retry is only possible for FAILED status."));
        }
    }
}
