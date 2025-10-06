package com.memorylab.controller;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.BoardRepository;
import com.memorylab.domain.board.Visibility;
import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Objects;

@Slf4j
@Controller
@RequestMapping("/api/thumb")
@RequiredArgsConstructor
public class ThumbnailProxyController {

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/{boardId}")
    public ResponseEntity<Void> getThumbnail(@PathVariable Long boardId, @AuthenticationPrincipal UserDetails userDetails, HttpServletResponse response) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("Board not found"));

        String thumbnailUrl = board.getThumbnailPath();
        if (thumbnailUrl == null) {
            return ResponseEntity.notFound().build();
        }

        // 공개 게시물인 경우, 바로 파일 경로로 리다이렉트
        if (board.getVisibility() == Visibility.PUBLIC) {
            response.setHeader("Location", thumbnailUrl);
            return new ResponseEntity<>(HttpStatus.FOUND); // 302 Redirect
        }

        // 비공개 게시물인 경우, 권한 확인
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Member currentUser = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.equals("ROLE_ADMIN"));
        boolean isOwner = Objects.equals(board.getUser().getId(), currentUser.getId());

        if (isAdmin || isOwner) {
            // 권한이 있으면 Nginx의 X-Accel-Redirect를 통해 내부적으로 파일 전달
            String internalRedirectUrl = "/protected" + thumbnailUrl.substring("/uploads".length());
            log.debug("Internal redirect for private thumbnail: {} -> {}", thumbnailUrl, internalRedirectUrl);
            response.setHeader("X-Accel-Redirect", internalRedirectUrl);
            response.setHeader("Content-Type", "image/jpeg");
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
