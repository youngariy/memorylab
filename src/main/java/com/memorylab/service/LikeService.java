package com.memorylab.service;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardLike;
import com.memorylab.domain.board.BoardLikeId;
import com.memorylab.domain.board.BoardLikeRepository;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.user.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    // 1. 결과를 담을 전용 DTO(record) 정의
    public record LikeResult(boolean isLiked, long likeCount) {}

    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;

    @Transactional
    public LikeResult toggleLike(Long boardId, Member user) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid board ID: " + boardId));

        BoardLikeId boardLikeId = new BoardLikeId(boardId, user.getId());

        boolean isLiked;

        // 2. 이미 좋아요를 눌렀는지 확인 후, 추가 또는 삭제
        if (boardLikeRepository.findById(boardLikeId).isPresent()) {
            boardLikeRepository.deleteById(boardLikeId);
            isLiked = false; // 좋아요 취소됨
        } else {
            BoardLike boardLike = new BoardLike(board, user);
            boardLikeRepository.save(boardLike);
            isLiked = true; // 좋아요 추가됨
        }

        // 3. [핵심] DB에 변경사항을 즉시 동기화
        boardLikeRepository.flush();

        // 4. [핵심] 동기화된 DB에서 최신 개수를 직접 조회
        long likeCount = boardLikeRepository.countByBoardId(boardId);

        // 5. 정확한 최종 결과 반환
        return new LikeResult(isLiked, likeCount);
    }
}
