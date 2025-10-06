package com.memorylab.controller;

import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import com.memorylab.dto.comment.CommentCreateRequestDto;
import com.memorylab.dto.comment.CommentResponseDto;
import com.memorylab.dto.comment.CommentUpdateRequestDto;
import com.memorylab.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final MemberRepository memberRepository;

    @PostMapping("/board/{boardId}/comments")
    public ResponseEntity<CommentResponseDto> createComment(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommentCreateRequestDto requestDto) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userDetails.getUsername()));
        CommentResponseDto responseDto = commentService.createComment(boardId, member, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/board/{boardId}/comments")
    public ResponseEntity<Page<CommentResponseDto>> getComments(
            @PathVariable Long boardId,
            Pageable pageable) {

        Page<CommentResponseDto> comments = commentService.getComments(boardId, pageable);
        return ResponseEntity.ok(comments);
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponseDto> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommentUpdateRequestDto requestDto) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userDetails.getUsername()));

        CommentResponseDto responseDto = commentService.updateComment(commentId, member, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userDetails.getUsername()));
        commentService.deleteComment(commentId, member);
        return ResponseEntity.noContent().build();
    }
}
