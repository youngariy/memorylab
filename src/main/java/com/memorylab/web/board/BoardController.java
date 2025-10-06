package com.memorylab.web.board;

import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import com.memorylab.dto.board.BoardCreateRequestDto;
import com.memorylab.dto.board.BoardDetailResponseDto;
import com.memorylab.dto.board.BoardListResponseDto;
import com.memorylab.dto.board.BoardUpdateRequestDto;
import com.memorylab.service.LikeService;
import com.memorylab.service.board.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
            @RequestPart(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        BoardDetailResponseDto responseDto = boardService.createBoard(requestDto, member, videoFile, thumbnailFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/me")
    public ResponseEntity<Page<BoardListResponseDto>> getMyBoardList(
            @PageableDefault(size = 12, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long meId = memberRepository.findByEmail(userDetails.getUsername())
                .map(Member::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // '내 글' 전용 서비스 메서드 호출로 수정
        return ResponseEntity.ok(boardService.getMyBoardList(pageable, meId));
    }

    @GetMapping
    public ResponseEntity<Page<BoardListResponseDto>> getBoardList(
            @PageableDefault(size = 12, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = (userDetails == null) ? null :
                memberRepository.findByEmail(userDetails.getUsername()).map(Member::getId).orElse(null);
        
        // '전체 글' 서비스 메서드 호출 (userId는 '좋아요' 표시에만 사용)
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

    @PutMapping(value = "/{boardId}", consumes = {"multipart/form-data"})
    public ResponseEntity<BoardDetailResponseDto> updateBoard(
            @PathVariable Long boardId,
            @RequestPart("req") BoardUpdateRequestDto requestDto,
            @RequestPart(value = "videoFile", required = false) MultipartFile videoFile,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Member member = memberRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        BoardDetailResponseDto responseDto = boardService.updateBoard(boardId, requestDto, member, videoFile);
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
