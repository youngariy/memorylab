package com.memorylab.service;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.comment.Comment;
import com.memorylab.domain.comment.CommentRepository;
import com.memorylab.domain.user.Member;
import com.memorylab.dto.comment.CommentCreateRequestDto;
import com.memorylab.dto.comment.CommentResponseDto;
import com.memorylab.dto.comment.CommentUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public CommentResponseDto createComment(Long boardId, Member user, CommentCreateRequestDto requestDto) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid board ID: " + boardId));

        Comment parent = null;
        if (requestDto.parentId() != null) {
            parent = commentRepository.findById(requestDto.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid parent comment ID: " + requestDto.parentId()));
        }

        Comment comment = Comment.builder()
                .board(board)
                .user(user)
                .parent(parent)
                .content(requestDto.content())
                .build();

        commentRepository.save(comment);

        return CommentResponseDto.fromEntity(comment);
    }

    public Page<CommentResponseDto> getComments(Long boardId, Pageable pageable) {
        if (!boardRepository.existsById(boardId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid board ID: " + boardId);
        }
        return commentRepository.findByBoardId(boardId, pageable)
                .map(CommentResponseDto::fromEntity);
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, Member user, CommentUpdateRequestDto requestDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found with id: " + commentId));

        // 권한 확인: 사용자가 댓글 작성자이거나, 관리자일 경우에만 통과
        boolean isOwner = Objects.equals(comment.getUser().getId(), user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this comment.");
        }

        comment.updateContent(requestDto.content());
        Comment updatedComment = commentRepository.save(comment);

        return CommentResponseDto.fromEntity(updatedComment);
    }

    @Transactional
    public void deleteComment(Long commentId, Member user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid comment ID: " + commentId));

        // 권한 확인: 사용자가 댓글 작성자이거나, 관리자일 경우에만 통과
        boolean isOwner = Objects.equals(comment.getUser().getId(), user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(role -> role.equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this comment.");
        }

        commentRepository.delete(comment);
    }
}
