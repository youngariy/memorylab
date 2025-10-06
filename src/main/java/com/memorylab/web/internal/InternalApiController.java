package com.memorylab.web.internal;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.TranscodeStatus;
import com.memorylab.dto.internal.ConversionCallbackRequest;
import com.memorylab.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalApiController {

    private final BoardRepository boardRepository;
    private final ThumbnailService thumbnailService;

    /**
     * AI 서버로부터 동영상 변환 결과 콜백을 받는 엔드포인트입니다.
     */
    @PostMapping("/conversion-callback")
    @Transactional
    public ResponseEntity<String> handleConversionCallback(@RequestBody ConversionCallbackRequest request) {
        log.info("Received conversion callback: {}", request);

        Board board = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID: " + request.boardId()));

        if ("COMPLETED".equalsIgnoreCase(request.status())) {
            board.setTranscodeStatus(TranscodeStatus.READY);
            board.setConvertedVideoPath(request.resultUrl());
            log.info("Board {} transcoding completed. Path: {}", board.getId(), request.resultUrl());

            // 동영상 변환이 성공했으므로, 이제 썸네일 생성을 요청합니다.
            thumbnailService.generateThumbnailAsync(board.getId());

        } else if ("FAILED".equalsIgnoreCase(request.status())) {
            board.setTranscodeStatus(TranscodeStatus.FAILED);
            log.error("Board {} transcoding failed. Reason: {}", board.getId(), request.errorMessage());
        }

        boardRepository.save(board);

        return ResponseEntity.ok("Callback processed.");
    }
}
