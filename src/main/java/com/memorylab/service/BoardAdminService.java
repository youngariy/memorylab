package com.memorylab.service;

import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.ExternalStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardAdminService {

    private final BoardRepository boardRepository;

    @Transactional
    public void cancelConversion(Long boardId) {
        boardRepository.findById(boardId).ifPresent(board -> {
            ExternalStatus currentStatus = board.getExternalStatus();
            // 외부 AI 서버에 요청이 아직 전송되지 않았거나, 이미 최종 상태가 아닌 경우에만 취소 가능
            if (currentStatus == null || !currentStatus.isTerminal()) {
                board.setExternalStatus(ExternalStatus.CANCELED);
                log.info("AI conversion for boardId: {} has been manually cancelled by an admin.", boardId);
            }
        });
    }
}
