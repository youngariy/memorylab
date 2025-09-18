// src/main/java/com/memorylab/service/board/BoardService.java
package com.memorylab.service.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ConversionStatus;
import com.memorylab.domain.board.Visibility;
import com.memorylab.domain.user.User;
import com.memorylab.dto.InternalDtos.ConversionCallbackRequest;
import com.memorylab.dto.board.BoardDtos.*;
import com.memorylab.repository.board.BoardRepository;
import com.memorylab.repository.user.UserRepository;
import com.memorylab.service.LocalFileService;
import com.memorylab.service.mail.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boards;
    private final UserRepository users;
    private final LocalFileService localFileService;
    private final RestTemplate restTemplate;
    private final EmailService mail;

    @Value("${app.upload.resource-handler}")
    private String resourceHandler;

    @Value("${app.ai-server.url}")
    private String aiServerUrl;

    public Long create(Long userId, CreateReq req, MultipartFile videoFile) {
        User author = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유저 없음"));

        Category category = Category.parse(req.category());
        if (category == Category.NOTICE && !author.getRoles().contains("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "공지사항은 관리자만 작성할 수 있습니다.");
        }

        Board.BoardBuilder builder = Board.builder()
                .author(author)
                .title(nz(req.title()))
                .content(nz(req.content()))
                .category(category)
                .visibility(Visibility.parse(req.visibility()))
                .tags(req.tags());

        if (videoFile != null && !videoFile.isEmpty()) {
            String storedFileName = localFileService.storeFile(videoFile);
            String videoUrl = resourceHandler.replace("/**", "/") + storedFileName;
            builder.videoUrl(videoUrl)
                   .conversionStatus(ConversionStatus.UPLOADED);
        }

        Board b = builder.build();
        boards.save(b);

        if (b.getConversionStatus() == ConversionStatus.UPLOADED) {
            triggerAiConversion(b.getId(), b.getVideoUrl());
        }

        return b.getId();
    }

    @Async
    public void triggerAiConversion(Long boardId, String videoUrl) {
        log.info("AI 변환 요청 시작: boardId={}, videoUrl={}", boardId, videoUrl);
        try {
            Map<String, Object> requestBody = Map.of("boardId", boardId, "videoUrl", videoUrl);
            restTemplate.postForObject(aiServerUrl, requestBody, String.class);
            log.info("AI 변환 요청 성공: boardId={}", boardId);
        } catch (Exception e) {
            log.error("AI 변환 요청 실패: boardId={}, 오류: {}", boardId, e.getMessage());
            boards.findById(boardId).ifPresent(board -> {
                board.updateConversionStatus(ConversionStatus.FAILED);
                boards.save(board);
            });
        }
    }

    @Transactional(readOnly = true)
    public Page<SummaryRes> list(String q, String category, String tag, Long authorId, Long meId, Pageable pageable) {
        Category cat = Category.parse(category);

        // === 관리자 여부 확인 로직 추가 ===
        boolean isAdmin = false;
        if (meId != null) {
            isAdmin = users.findById(meId)
                           .map(user -> user.getRoles().contains("ROLE_ADMIN"))
                           .orElse(false);
        }
        // ===============================

        Page<Board> page = boards.search(q, cat, tag, authorId, meId, isAdmin, pageable); // isAdmin 파라미터 전달

        return page.map(b -> new SummaryRes(
                b.getId(),
                b.getTitle(),
                b.getCategory().name(),
                b.getVisibility().name(),
                b.getThumbnailUrl(),
                b.getConversionStatus().name(),
                b.getTags(),
                b.getViewCount(),
                b.getCreatedAt(),
                b.getAuthor().getNickname()
        ));
    }

    @Transactional(readOnly = false)
    public DetailRes read(Long id, Long meId, boolean increaseView) {
        Board b = boards.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));

        // === '나만보기' 글에 대한 관리자 열람 권한 처리 ===
        if (b.getVisibility() == Visibility.PRIVATE) {
            boolean isOwner = (meId != null && b.isAuthor(meId));
            // 관리자 여부 확인
            boolean isAdmin = (meId != null && users.findById(meId)
                                                    .map(u -> u.getRoles().contains("ROLE_ADMIN"))
                                                    .orElse(false));

            if (!isOwner && !isAdmin) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 글입니다.");
            }
        }
        // ===========================================

        if (increaseView) b.increaseView();

        return new DetailRes(
                b.getId(), b.getTitle(), b.getContent(),
                b.getCategory().name(), b.getVisibility().name(),
                b.getVideoUrl(), b.getThumbnailUrl(), b.getTags(),
                b.getConversionStatus().name(), b.getViewCount(),
                b.getCreatedAt(), b.getUpdatedAt(),
                b.getAuthor().getId(), b.getAuthor().getNickname()
        );
    }

    public void update(Long id, Long userId, UpdateReq req, MultipartFile videoFile) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유저 없음"));
        Board b = boards.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));

        if (!b.isAuthor(userId) && !user.getRoles().contains("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        }

        b.modify(
                nz(req.title()),
                nz(req.content()),
                Category.parse(req.category()),
                Visibility.parse(req.visibility()),
                req.tags()
        );

        if (videoFile != null && !videoFile.isEmpty()) {
            String storedFileName = localFileService.storeFile(videoFile);
            String videoUrl = resourceHandler.replace("/**", "/") + storedFileName;
            b.changeVideo(videoUrl);
            triggerAiConversion(b.getId(), b.getVideoUrl());
        }
    }
    
    public void processConversionResult(ConversionCallbackRequest request) {
        log.info("변환 결과 콜백 수신: boardId={}, status={}", request.getBoardId(), request.getStatus());

        boards.findById(request.getBoardId()).ifPresentOrElse(
            board -> {
                User author = board.getAuthor();
                if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
                    board.updateConversionResult(ConversionStatus.COMPLETED, request.getThumbnailUrl());
                    
                    String subject = "MemoryLab 동영상 변환이 완료되었습니다!";
                    String body = String.format(
                        "안녕하세요, %s님!\n\n게시글 '%s'에 업로드하신 동영상의 3D 모델 변환이 성공적으로 완료되었습니다.\n\n지금 바로 확인해보세요!\n%s",
                        author.getNickname(),
                        board.getTitle(),
                        "http://your-service-domain.com/board/view?id=" + board.getId() // 중요: 실제 서비스 도메인으로 변경
                    );
                    mail.send(author.getEmail(), subject, body);
                    
                } else if ("FAILED".equalsIgnoreCase(request.getStatus())) {
                    board.updateConversionStatus(ConversionStatus.FAILED);

                    String subject = "MemoryLab 동영상 변환에 실패했습니다.";
                    String body = String.format(
                        "안녕하세요, %s님.\n\n아쉽게도 게시글 '%s'에 업로드하신 동영상의 3D 모델 변환에 실패했습니다.\n\n다시 시도해주시거나 관리자에게 문의해주세요.",
                        author.getNickname(),
                        board.getTitle()
                    );
                    mail.send(author.getEmail(), subject, body);
                }
            },
            () -> log.error("변환 결과 콜백 처리 실패: boardId {} 에 해당하는 게시글을 찾을 수 없습니다.", request.getBoardId())
        );
    }

    public void delete(Long id, Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유저 없음"));
        Board b = boards.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));

        if (!b.isAuthor(userId) && !user.getRoles().contains("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        if (StringUtils.hasText(b.getVideoUrl())) {
            String filename = b.getVideoUrl().substring(b.getVideoUrl().lastIndexOf("/") + 1);
            localFileService.deleteFile(filename);
        }

        boards.delete(b);
    }

    private String nz(String s) { return (s == null) ? "" : s.trim(); }
}
