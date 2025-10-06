package com.memorylab.service.board;

import com.memorylab.domain.board.*;
import com.memorylab.domain.user.Member;
import com.memorylab.dto.board.BoardCreateRequestDto;
import com.memorylab.dto.board.BoardDetailResponseDto;
import com.memorylab.dto.board.BoardListResponseDto;
import com.memorylab.dto.board.BoardUpdateRequestDto;
import com.memorylab.service.FileService;
import com.memorylab.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final FileService fileService;
    private final ThumbnailService thumbnailService;
    private final BoardLikeRepository boardLikeRepository;

    @Transactional
    public BoardDetailResponseDto createBoard(BoardCreateRequestDto requestDto, Member user, MultipartFile videoFile, MultipartFile thumbnailFile) throws IOException {
        Board board = requestDto.toEntity(user);
        boardRepository.save(board);
        log.info("Saved initial board with id: {}", board.getId());

        if (videoFile != null && !videoFile.isEmpty()) {
            Path videoPath = fileService.getVideoPath(board.getId());
            fileService.saveFile(videoFile, videoPath);
            board.setOriginalVideoPath(videoPath.toString());
            board.setTranscodeStatus(TranscodeStatus.PENDING);

            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                log.info("Client-provided thumbnail exists for boardId: {}. Saving it.", board.getId());
                Path thumbnailPath = fileService.getThumbnailPath(board.getId());
                fileService.saveFile(thumbnailFile, thumbnailPath);
                board.setThumbnailPath(fileService.getRelativeThumbnailUrl(board.getId()));
                board.setThumbnailStatus(ThumbnailStatus.READY);
            } else {
                thumbnailService.attemptImmediateGeneration(board);
            }
        }

        Board savedBoard = boardRepository.save(board);
        return BoardDetailResponseDto.fromEntity(savedBoard, false);
    }

    @Transactional
    public void deleteBoard(Long boardId, Member user) {
        var opt = boardRepository.findById(boardId);
        if (opt.isEmpty()) {
            log.warn("Attempted to delete non-existent board id={}", boardId);
            return;
        }

        Board board = opt.get();

        boolean isAdmin = user.getRoles().stream().anyMatch("ROLE_ADMIN"::equals);
        if (!Objects.equals(board.getUser().getId(), user.getId()) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this post.");
        }

        boardRepository.delete(board);

        if (board.getOriginalVideoPath() != null) {
            try {
                fileService.deleteDirectory(fileService.getVideoDirectory(boardId));
                fileService.deleteDirectory(fileService.getThumbnailDirectory(boardId));
                log.info("Deleted associated files/directories for boardId={}", boardId);
            } catch (Exception e) {
                log.error("File cleanup failed for boardId={}: {}", boardId, e.getMessage(), e);
            }
        }
    }

    public Page<BoardListResponseDto> getBoardList(Pageable pageable, Long userId) {
        Pageable p = pageable;
        if (p.getSort().isUnsorted()) {
            p = PageRequest.of(p.getPageNumber(), p.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }
        Page<Board> boards = boardRepository.findAll(p);

        Set<Long> likedBoardIds = getLikedBoardIds(boards.getContent(), userId);
        return boards.map(b -> BoardListResponseDto.fromEntity(b, likedBoardIds.contains(b.getId())));
    }

    public Page<BoardListResponseDto> getMyBoardList(Pageable pageable, Long meId) {
        Pageable p = pageable;
        if (p.getSort().isUnsorted()) {
            p = PageRequest.of(p.getPageNumber(), p.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }
        Page<Board> boards = boardRepository.findByUser_Id(meId, p);
        Set<Long> likedBoardIds = getLikedBoardIds(boards.getContent(), meId);
        return boards.map(b -> BoardListResponseDto.fromEntity(b, likedBoardIds.contains(b.getId())));
    }

    @Transactional
    public BoardDetailResponseDto getBoardDetail(Long boardId, Long userId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found with id: " + boardId));
        board.increaseViewCount();
        Set<Long> likedBoardIds = getLikedBoardIds(List.of(board), userId);
        return BoardDetailResponseDto.fromEntity(board, likedBoardIds.contains(board.getId()));
    }

    @Transactional
    public BoardDetailResponseDto updateBoard(Long boardId, BoardUpdateRequestDto requestDto, Member user, MultipartFile videoFile) throws IOException {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found with id: " + boardId));

        boolean isAdmin = user.getRoles().stream().anyMatch("ROLE_ADMIN"::equals);
        if (!Objects.equals(board.getUser().getId(), user.getId()) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this post.");
        }

        board.setTitle(requestDto.title());
        board.setContent(requestDto.content());
        board.setCategory(requestDto.category());
        board.setVisibility(requestDto.visibility());

        if (videoFile != null && !videoFile.isEmpty()) {
            fileService.deleteDirectory(fileService.getVideoDirectory(boardId));
            fileService.deleteDirectory(fileService.getThumbnailDirectory(boardId));

            Path videoPath = fileService.getVideoPath(board.getId());
            fileService.saveFile(videoFile, videoPath);
            board.setOriginalVideoPath(videoPath.toString());
            board.setTranscodeStatus(TranscodeStatus.PENDING);
            board.setThumbnailStatus(ThumbnailStatus.PENDING);
            board.setThumbnailPath(null);
            board.setRetryCount(0);

            thumbnailService.attemptImmediateGeneration(board);
        }

        Board updatedBoard = boardRepository.save(board);
        Set<Long> likedBoardIds = getLikedBoardIds(List.of(updatedBoard), user.getId());
        return BoardDetailResponseDto.fromEntity(updatedBoard, likedBoardIds.contains(updatedBoard.getId()));
    }

    public int getLikeCount(Long boardId) {
        return boardRepository.findById(boardId)
                .map(Board::getLikeCount)
                .orElse(0);
    }

    private Set<Long> getLikedBoardIds(List<Board> boards, Long userId) {
        if (userId == null || boards.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> boardIds = boards.stream().map(Board::getId).collect(Collectors.toList());
        return boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds(userId, boardIds);
    }
}
