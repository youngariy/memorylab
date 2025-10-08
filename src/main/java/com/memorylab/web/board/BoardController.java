package com.memorylab.web.board;

import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import com.memorylab.dto.board.*;
import com.memorylab.service.LikeService;
import com.memorylab.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final MemberRepository memberRepository;
    private final LikeService likeService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<BoardDetailResponseDto> createBoard(
            @RequestPart("req") BoardCreateRequestDto requestDto,
            @RequestPart(value = "videoFile", required = false) MultipartFile videoFile,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // 수정: thumbnailFile 파라미터 제거
        BoardDetailResponseDto responseDto = boardService.createBoard(requestDto, member, videoFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/me")
    public ResponseEntity<BoardPageResponseDto> getMyBoardList(
            @PageableDefault(size = 12, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long meId = memberRepository.findByEmail(userDetails.getUsername())
                .map(Member::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(boardService.getMyBoardList(pageable, meId));
    }

    @GetMapping
    public ResponseEntity<BoardPageResponseDto> getBoardList(
            @PageableDefault(size = 12, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = (userDetails == null) ? null :
                memberRepository.findByEmail(userDetails.getUsername()).map(Member::getId).orElse(null);
        
        return ResponseEntity.ok(boardService.getBoardList(pageable, userId));
    }

    @GetMapping("/{boardId:\\d+}")
    public ResponseEntity<BoardDetailResponseDto> getBoardDetail(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = (userDetails == null) ? null :
                memberRepository.findByEmail(userDetails.getUsername()).map(Member::getId).orElse(null);
        BoardDetailResponseDto boardDetail = boardService.getBoardDetail(boardId, userId);
        return ResponseEntity.ok(boardDetail);
    }

    @PutMapping("/{boardId}") // 수정: multipart 제거
    public ResponseEntity<BoardDetailResponseDto> updateBoard(
            @PathVariable Long boardId,
            @RequestBody BoardUpdateRequestDto requestDto, // 수정: @RequestPart -> @RequestBody
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // 수정: videoFile 파라미터 제거
        BoardDetailResponseDto responseDto = boardService.updateBoard(boardId, requestDto, member);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long boardId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        boardService.deleteBoard(boardId, member);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{boardId}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long boardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        LikeService.LikeResult result = likeService.toggleLike(boardId, member);

        return ResponseEntity.ok(Map.of("isLiked", result.isLiked(), "likeCount", result.likeCount()));
    }
}
