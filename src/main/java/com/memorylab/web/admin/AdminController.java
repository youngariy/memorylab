package com.memorylab.web.admin;

import com.memorylab.domain.board.ConversionStatus;
import com.memorylab.dto.admin.AdminBoardDtos.AdminBoardSummaryRes;
import com.memorylab.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final BoardService boardService;

    @GetMapping("/boards")
    public ResponseEntity<Page<AdminBoardSummaryRes>> getBoardListForAdmin(
            @RequestParam(required = false) ConversionStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            Pageable pageable
    ) {
        Page<AdminBoardSummaryRes> list = boardService.listForAdmin(status, keyword, from, to, pageable);
        return ResponseEntity.ok(list);
    }

    @PostMapping("/boards/{id}/retry")
    public ResponseEntity<?> retryConversion(@PathVariable Long id) {
        boardService.retryConversion(id);
        return ResponseEntity.accepted().body(Map.of("status", "queued"));
    }

    /**
     * 변환 상태가 누락된(null) 과거 데이터를 찾아 PENDING 상태로 만들어 변환 큐에 등록합니다.
     * @return 백필 처리된 게시글의 수
     */
    @PostMapping("/boards/backfill")
    public ResponseEntity<?> triggerBackfill() {
        long count = boardService.backfillPendingConversions();
        return ResponseEntity.ok(Map.of("backfilledCount", count));
    }
}
