package com.memorylab.service.board;

import com.memorylab.ai.AiServerClient;
import com.memorylab.domain.BoardStatus;
import com.memorylab.domain.board.*;
import com.memorylab.domain.user.Member;
import com.memorylab.dto.board.*;
import com.memorylab.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final AiServerClient aiServerClient; // GpuApiService 대신 AiServerClient 주입
    private final BoardLikeRepository boardLikeRepository;

    @Value("${file.storage.ply-base-path}")
    private String plyBasePath;

    // --- 핵심 로직: 실시간 상태 매핑 --- //
    private BoardStatus mapToBoardStatus(Board b) {
        // 이 로직은 새로운 ExternalStatus와 연계하여 재검토될 수 있으나, 현재는 컴파일을 위해 유지합니다.
        TranscodeStatus t = b.getTranscodeStatus();
        ThumbnailStatus th = b.getThumbnailStatus();
        boolean hasThumb = b.getThumbnailPath() != null && !b.getThumbnailPath().isBlank();

        // 실패 우선
        if (t == TranscodeStatus.FAILED) return BoardStatus.FAILED_PROCESS;
        if (th == ThumbnailStatus.FAILED) return BoardStatus.FAILED_DOWNLOAD; // 썸네일 실패를 다운로드 실패로 매핑

        // 변환 진행/대기 (3D 모델 생성 이전 단계)
        if (t == TranscodeStatus.CONVERTING || t == TranscodeStatus.PENDING || t == TranscodeStatus.NONE) {
            return BoardStatus.PROCESSING; // AI 변환 대기/진행
        }

        // 변환 완료 후 후속 단계
        if (t == TranscodeStatus.READY) {
            if (th == ThumbnailStatus.READY && hasThumb) return BoardStatus.READY;
            if (!hasThumb) return BoardStatus.DOWNLOADING; // 썸네일 경로 아직 없음 (생성 중)
            return BoardStatus.RESULT_READY; // 썸네일 생성 대기/진행
        }

        // 업로드 직후 등 초기 상태
        return BoardStatus.DISPATCHED;
    }

    @Transactional
    public BoardDetailResponseDto createBoard(BoardCreateRequestDto requestDto, Member user, MultipartFile videoFile) throws IOException {
        Board board = requestDto.toEntity(user);
        boardRepository.save(board);
        log.info("Saved initial board with id: {}", board.getId());

        if (videoFile != null && !videoFile.isEmpty()) {
            String uniqueFilename = board.getId() + "_" + StringUtils.cleanPath(Objects.requireNonNull(videoFile.getOriginalFilename()));
            Path videoPath = fileService.getVideoPath(uniqueFilename);
            fileService.saveFile(videoFile, videoPath);
            board.setOriginalVideoPath(videoPath.toString());
            
            // 썸네일과 동영상 변환 상태를 PENDING으로 설정
            board.setThumbnailStatus(ThumbnailStatus.PENDING);
            board.setTranscodeStatus(TranscodeStatus.PENDING); // 이 상태를 기반으로 다른 서비스가 동작
        }

        Board savedBoard = boardRepository.save(board);
        return BoardDetailResponseDto.fromEntity(savedBoard, false, mapToBoardStatus(savedBoard));
    }

    @Transactional
    public void deleteBoard(Long boardId, Member user) {
        Board board = boardRepository.findByIdWithUser(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found"));

        boolean isAdmin = user.getRoles().stream().anyMatch("ROLE_ADMIN"::equals);
        if (!Objects.equals(board.getUser().getId(), user.getId()) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to delete this post.");
        }

        boardRepository.delete(board);

        try {
            if (board.getOriginalVideoPath() != null) {
                fileService.deleteDirectory(Paths.get(board.getOriginalVideoPath()));
            }
            if (board.getThumbnailPath() != null) {
                Path thumbnailDir = Paths.get(fileService.getUploadRootDir().toString(), "thumbnails");
                fileService.deleteDirectory(thumbnailDir.resolve(board.getId() + ".jpg"));
            }
            if (board.getPlyPath() != null) {
                Path plyDir = Paths.get(plyBasePath, board.getId().toString());
                fileService.deleteDirectory(plyDir);
            }
            // AI 서버에 생성된 리소스 삭제 요청
            if (board.getAiTaskId() != null) {
                aiServerClient.deleteAiResource(board.getAiTaskId());
            }
            log.info("Cleaned up resources for deleted boardId={}", boardId);
        } catch (Exception e) {
            log.error("Resource cleanup failed for boardId={}: {}", boardId, e.getMessage(), e);
        }
    }

    public BoardPageResponseDto getBoardList(Pageable pageable, Long userId) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findAll(p);
        Set<Long> likedBoardIds = getLikedBoardIds(boards.getContent(), userId);
        Page<BoardListResponseDto> dtoPage = boards.map(b -> BoardListResponseDto.fromEntity(b, likedBoardIds.contains(b.getId()), mapToBoardStatus(b)));
        return BoardPageResponseDto.fromPage(dtoPage);
    }

    public BoardPageResponseDto getMyBoardList(Pageable pageable, Long meId) {
        Pageable p = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Board> boards = boardRepository.findByUser_Id(meId, p);
        Set<Long> likedBoardIds = getLikedBoardIds(boards.getContent(), meId);
        Page<BoardListResponseDto> dtoPage = boards.map(b -> BoardListResponseDto.fromEntity(b, likedBoardIds.contains(b.getId()), mapToBoardStatus(b)));
        return BoardPageResponseDto.fromPage(dtoPage);
    }

    @Transactional
    public BoardDetailResponseDto getBoardDetail(Long boardId, Long userId) {
        Board board = boardRepository.findByIdWithUser(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found with id: " + boardId));
        board.increaseViewCount();
        Set<Long> likedBoardIds = getLikedBoardIds(List.of(board), userId);
        return BoardDetailResponseDto.fromEntity(board, likedBoardIds.contains(board.getId()), mapToBoardStatus(board));
    }

    @Transactional
    public BoardDetailResponseDto updateBoard(Long boardId, BoardUpdateRequestDto requestDto, Member user) {
        Board board = boardRepository.findByIdWithUser(boardId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Board not found with id: " + boardId));

        boolean isAdmin = user.getRoles().stream().anyMatch("ROLE_ADMIN"::equals);
        if (!Objects.equals(board.getUser().getId(), user.getId()) && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to edit this post.");
        }

        board.setTitle(requestDto.title());
        board.setContent(requestDto.content());
        board.setCategory(requestDto.category());
        board.setVisibility(requestDto.visibility());

        Board updatedBoard = boardRepository.save(board);
        Set<Long> likedBoardIds = getLikedBoardIds(List.of(updatedBoard), user.getId());
        return BoardDetailResponseDto.fromEntity(updatedBoard, likedBoardIds.contains(updatedBoard.getId()), mapToBoardStatus(updatedBoard));
    }

    private Set<Long> getLikedBoardIds(List<Board> boards, Long userId) {
        if (userId == null || boards.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> boardIds = boards.stream().map(Board::getId).collect(Collectors.toList());
        return boardLikeRepository.findLikedBoardIdsByUserIdAndBoardIds(userId, boardIds);
    }
}
