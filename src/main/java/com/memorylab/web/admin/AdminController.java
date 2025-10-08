package com.memorylab.web.admin;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BoardRepository boardRepository;

    @PostMapping("/board/reset-conversion")
    @Transactional
    public String resetConversionStatus(@RequestParam("boardId") Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));

        // TODO: 새로운 프로토콜에 맞는 재처리 로직 구현 필요 (예: GPU 서버에 다시 업로드 요청)
        // 현재는 낡은 상태 변경 로직을 제거하여 컴파일 오류만 해결합니다.
        // if (board.getStatus() == BoardStatus.FAILED_PROCESS) {
        //     board.setStatus(BoardStatus.DISPATCHED);
        // }

        boardRepository.save(board);
        return "redirect:/board/view?id=" + boardId;
    }
}
