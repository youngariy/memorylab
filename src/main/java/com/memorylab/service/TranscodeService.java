package com.memorylab.service;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.TranscodeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodeService {

    private final BoardRepository boardRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.ai-server.url}")
    private String aiServerUrl;

    @Transactional
    public void triggerAiConversion(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID: " + boardId));

        if (board.getTranscodeStatus() != TranscodeStatus.PENDING) {
            log.warn("Board {} is not in PENDING state for transcoding.", boardId);
            return;
        }

        try {
            board.setTranscodeStatus(TranscodeStatus.CONVERTING);
            board.increaseRetryCount(); // 재시도 횟수 증가
            boardRepository.save(board);

            String filePath = board.getOriginalVideoPath();
            log.info("Requesting AI conversion for boardId: {}, filePath: {}", boardId, filePath);

            // AI 서버에 변환 요청
            restTemplate.postForObject(aiServerUrl, Map.of("boardId", boardId, "videoPath", filePath), String.class);

        } catch (Exception e) {
            log.error("Error triggering AI conversion for boardId: {}", boardId, e);
            board.setTranscodeStatus(TranscodeStatus.FAILED);
            boardRepository.save(board);
        }
    }
}
