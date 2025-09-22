package com.memorylab.service.board;

import com.memorylab.domain.board.Board;
import com.memorylab.domain.board.Category;
import com.memorylab.domain.board.ConversionStatus;
import com.memorylab.domain.board.Visibility;
import com.memorylab.domain.user.User;
import com.memorylab.dto.InternalDtos.AiServerStatusResponse;
import com.memorylab.dto.InternalDtos.ConversionCallbackRequest;
import com.memorylab.dto.InternalDtos.ConversionProgressRequest;
import com.memorylab.dto.admin.AdminBoardDtos.AdminBoardSummaryRes;
import com.memorylab.dto.board.BoardDtos.*;
import com.memorylab.metrics.ConversionMetricsService;
import com.memorylab.repository.board.BoardRepository;
import com.memorylab.repository.user.UserRepository;
import com.memorylab.service.FileService;
import com.memorylab.service.FileType;
import com.memorylab.service.mail.EmailService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boards;
    private final UserRepository users;
    private final FileService fileService;
    private final RestTemplate restTemplate;
    private final EmailService mail;
    private final ConversionMetricsService metricsService;

    @Value("${app.upload.root-dir}")
    private String rootDir;
    @Value("${app.upload.converted-base-url}")
    private String convertedBaseUrl;
    @Value("${app.ai-server.url}")
    private String aiServerUrl;
    @Value("${app.ai-server.status-url}")
    private String aiServerStatusUrl;
    @Value("${app.scheduler.max-retries:3}")
    private int maxRetries;

    public Long create(Long userId, CreateReq req, MultipartFile videoFile) {
        User author = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "유저 없음"));
        Board.BoardBuilder builder = Board.builder().author(author).title(nz(req.title())).content(nz(req.content()))
                .category(Category.parse(req.category())).visibility(Visibility.parse(req.visibility())).tags(req.tags());

        if (videoFile != null && !videoFile.isEmpty()) {
            String relativePath = fileService.storeFile(videoFile, FileType.ORIGINAL);
            builder.originalVideoPath(relativePath);
            Board b = builder.build();
            b.changeVideo(relativePath);
            boards.save(b);
            metricsService.incrementPending();
            return b.getId();
        }
        Board b = builder.build();
        boards.save(b);
        return b.getId();
    }

    @CircuitBreaker(name = "ai-server", fallbackMethod = "handleAiServerFailure")
    public void triggerAiConversion(Long boardId) {
        Board board = boards.findById(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        board.incrementRetryCount();
        board.updateConversionStatus(ConversionStatus.PROCESSING);

        String fullPhysicalPath = Paths.get(rootDir).resolve(board.getOriginalVideoPath()).toAbsolutePath().toString();
        log.info("AI 변환 요청 시작: boardId={}, jobId={}, physicalPath={}, retryCount={}", boardId, board.getJobId(), fullPhysicalPath, board.getRetryCount());
        restTemplate.postForObject(aiServerUrl, Map.of("boardId", boardId, "jobId", board.getJobId(), "videoPath", fullPhysicalPath), String.class);
    }

    @CircuitBreaker(name = "ai-server", fallbackMethod = "handleAiServerFailure")
    public void checkConversionStatus(Long boardId) {
        Board board = boards.findById(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        String jobId = board.getJobId();
        if (!StringUtils.hasText(jobId)) {
            log.warn("[POLLING] boardId={}에 유효한 jobId가 없어 폴링을 건너뜁니다.", boardId);
            return;
        }

        log.info("[POLLING] AI 서버에 상태 확인 요청: boardId={}, jobId={}", boardId, jobId);
        AiServerStatusResponse response = restTemplate.getForObject(aiServerStatusUrl, AiServerStatusResponse.class, Map.of("boardId", boardId, "jobId", jobId));

        if (response == null || !jobId.equals(response.getJobId())) {
            log.warn("[POLLING] 수신된 jobId가 현재 작업과 일치하지 않거나 응답이 유효하지 않습니다.");
            return;
        }

        if (board.getConversionStatus() != ConversionStatus.PROCESSING) return;

        try {
            switch (response.getStatus()) {
                case "processing" -> board.updateProgress(response.getPercentage());
                case "error" -> board.updateOnError(response.getErrorMessage());
                case "completed" -> ingestAndSaveCompletedFiles(board, response.getThumbnailUrl(), response.getResultUrl());
            }
        } catch (OptimisticLockingFailureException e) {
            log.warn("[POLLING] 상태 업데이트 중 충돌 발생. 다음 주기에 재시도됩니다. boardId={}, jobId={}", boardId, jobId);
        }
    }

    public void processConversionResult(ConversionCallbackRequest request) {
        log.info("변환 결과 콜백 수신: boardId={}, jobId={}, status={}", request.getBoardId(), request.getJobId(), request.getStatus());
        boards.findById(request.getBoardId()).ifPresentOrElse(board -> {
            if (!request.getJobId().equals(board.getJobId()) || board.getConversionStatus() != ConversionStatus.PROCESSING) {
                log.warn("[CALLBACK] 수신된 jobId가 현재 작업과 일치하지 않거나 상태가 PROCESSING이 아니므로 결과를 무시합니다.");
                return;
            }

            try {
                if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
                    ingestAndSaveCompletedFiles(board, request.getThumbnailUrl(), request.getConvertedVideoUrl());
                } else {
                    board.updateOnError(StringUtils.hasText(request.getErrorMessage()) ? request.getErrorMessage() : "AI 서버에서 변환 실패 응답을 보냈습니다.");
                    metricsService.incrementError();
                    sendEmail(board.getAuthor(), board.getTitle(), board.getId(), false);
                }
            } catch (OptimisticLockingFailureException e) {
                log.warn("[CALLBACK] 상태 업데이트 중 충돌 발생. 폴링 스케줄러가 처리할 것입니다.", e);
            }
        }, () -> log.error("콜백 처리 실패: boardId {}를 찾을 수 없습니다.", request.getBoardId()));
    }

    public void updateConversionProgress(ConversionProgressRequest request) {
        log.info("변환 진행률 콜백 수신: boardId={}, jobId={}, progress={}%", request.getBoardId(), request.getJobId(), request.getProgress());
        boards.findById(request.getBoardId()).ifPresentOrElse(board -> {
            if (!request.getJobId().equals(board.getJobId()) || board.getConversionStatus() != ConversionStatus.PROCESSING) {
                log.warn("[PROGRESS_CALLBACK] 수신된 jobId가 현재 작업과 일치하지 않거나 상태가 PROCESSING이 아니므로 진행률 업데이트를 무시합니다.");
                return;
            }

            try {
                board.updateProgress(request.getProgress());
            } catch (OptimisticLockingFailureException e) {
                log.warn("[PROGRESS_CALLBACK] 진행률 업데이트 중 충돌 발생. 폴링 스케줄러가 처리할 것입니다.", e);
            }
        }, () -> log.error("진행률 콜백 처리 실패: boardId {}를 찾을 수 없습니다.", request.getBoardId()));
    }

    private void ingestAndSaveCompletedFiles(Board board, String thumbnailUrl, String convertedVideoUrl) {
        try {
            log.info("결과 파일 반입 시작: boardId={}, jobId={}", board.getId(), board.getJobId());
            String thumbnailPath = ingestFileFromUrl(thumbnailUrl, FileType.THUMBNAIL);
            String convertedVideoPath = ingestFileFromUrl(convertedVideoUrl, FileType.CONVERTED);

            board.updateOnCompletion(thumbnailPath, convertedVideoPath);
            metricsService.incrementCompleted();
            sendEmail(board.getAuthor(), board.getTitle(), board.getId(), true);
            log.info("결과 파일 반입 및 DB 업데이트 성공: boardId={}", board.getId());
        } catch (Exception e) {
            log.error("결과 파일 반입 중 심각한 오류 발생: boardId={}, error={}", board.getId(), e.getMessage());
            board.updateOnError("결과 파일 다운로드 또는 저장에 실패했습니다.");
            metricsService.incrementError();
        }
    }

    private String ingestFileFromUrl(String fileUrl, FileType fileType) {
        if (!StringUtils.hasText(fileUrl)) return null;

        try {
            // TODO: 대용량 파일을 위해 스트리밍 API(restTemplate.execute)로 변경해야 합니다.
            byte[] fileData = restTemplate.getForObject(fileUrl, byte[].class);
            if (fileData == null) {
                throw new RuntimeException("파일 다운로드 실패: 응답 본문이 비어있습니다.");
            }
            String originalFileName = UriComponentsBuilder.fromHttpUrl(fileUrl).build().getPathSegments().getLast();
            return fileService.storeFile(fileData, fileType, originalFileName);
        } catch (Exception e) {
            log.error("URL로부터 파일 저장 실패: url={}, error={}", fileUrl, e.getMessage());
            throw new RuntimeException("URL로부터 파일을 저장할 수 없습니다.", e);
        }
    }

    public void handleAiServerFailure(Long boardId, Throwable t) {
        log.warn("[FALLBACK] 서킷 브레이커 활성화. boardId={}, error: {}", boardId, t.getMessage());
        Board board = boards.findById(boardId).orElse(null);
        if (board != null && board.getConversionStatus() == ConversionStatus.PROCESSING) {
            board.updateConversionStatus(ConversionStatus.PENDING);
        }
    }

    public void markAsDeadLetter(Long boardId) {
        Board board = boards.findById(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        String errorMessage = String.format("최대 재시도 횟수(%d회)를 초과하여 영구 실패로 처리되었습니다.", maxRetries);
        board.updateOnError(errorMessage);
        metricsService.incrementError();
    }

    public long backfillPendingConversions() {
        List<Board> targets = boards.findByOriginalVideoPathIsNotNullAndConversionStatusIsNull();
        if (targets.isEmpty()) return 0;
        log.info("[BACKFILL] {}개 작업 백필 시작", targets.size());
        for (Board board : targets) {
            board.updateConversionStatus(ConversionStatus.PENDING);
            metricsService.incrementPending();
        }
        return targets.size();
    }

    @Transactional(readOnly = true)
    public Page<AdminBoardSummaryRes> listForAdmin(ConversionStatus status, String keyword, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        Page<Board> page = boards.searchForAdmin(status, keyword, from, to, pageable);
        return page.map(b -> new AdminBoardSummaryRes(b.getId(), b.getTitle(), b.getAuthor().getNickname(),
                b.getConversionStatus() != null ? b.getConversionStatus().name() : null,
                b.getProgress(), b.getErrorMessage(), b.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public Page<SummaryRes> list(String q, String category, String tag, Long authorId, Long meId, Pageable pageable) {
        Page<Board> page = boards.search(q, Category.parse(category), tag, authorId, meId, isAdmin(meId), pageable);
        return page.map(b -> new SummaryRes(b.getId(), b.getTitle(), b.getCategory().name(), b.getVisibility().name(),
                buildPublicUrl(b.getThumbnailPath()), b.getConversionStatus() != null ? b.getConversionStatus().name() : null,
                b.getProgress(), b.getErrorMessage(), b.getTags(), b.getViewCount(),
                b.getCreatedAt(), b.getAuthor().getNickname()));
    }

    @Transactional(readOnly = false)
    public DetailRes read(Long id, Long meId, boolean increaseView) {
        Board b = boards.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (b.getVisibility() == Visibility.PRIVATE && !isOwnerOrAdmin(b, meId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "비공개 글입니다.");
        }
        if (increaseView) b.increaseView();
        return new DetailRes(b.getId(), b.getTitle(), b.getContent(), b.getCategory().name(), b.getVisibility().name(),
                buildPublicUrl(b.getConvertedVideoPath()), buildPublicUrl(b.getThumbnailPath()), b.getTags(),
                b.getConversionStatus() != null ? b.getConversionStatus().name() : null, b.getProgress(),
                b.getErrorMessage(), b.getViewCount(), b.getCreatedAt(), b.getUpdatedAt(),
                b.getAuthor().getId(), b.getAuthor().getNickname());
    }

    public void update(Long id, Long userId, UpdateReq req, MultipartFile videoFile) {
        Board b = boards.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (!isOwnerOrAdmin(b, userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "수정 권한이 없습니다.");
        b.modify(nz(req.title()), nz(req.content()), Category.parse(req.category()), Visibility.parse(req.visibility()), req.tags());

        if (videoFile != null && !videoFile.isEmpty()) {
            deleteStoredVideos(b);
            String relativePath = fileService.storeFile(videoFile, FileType.ORIGINAL);
            b.changeVideo(relativePath);
            metricsService.incrementPending();
        }
    }

    public void retryConversion(Long boardId) {
        Board board = boards.findById(boardId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (!StringUtils.hasText(board.getOriginalVideoPath())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "재시도할 원본 동영상이 없습니다.");
        if (board.getConversionStatus() != ConversionStatus.ERROR) throw new ResponseStatusException(HttpStatus.CONFLICT, "재시도는 ERROR 상태인 작업만 가능합니다.");
        log.info("[AUDIT] 관리자(id:{})에 의해 변환 재시도 요청. boardId: {}", SecurityContextHolder.getContext().getAuthentication().getPrincipal(), boardId);
        board.resetForRetry();
        metricsService.incrementPending();
    }

    public void delete(Long id, Long userId) {
        Board b = boards.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글 없음"));
        if (!isOwnerOrAdmin(b, userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        deleteStoredVideos(b);
        boards.delete(b);
    }

    private void deleteStoredVideos(Board board) {
        if (StringUtils.hasText(board.getOriginalVideoPath())) fileService.deleteFile(board.getOriginalVideoPath());
        if (StringUtils.hasText(board.getConvertedVideoPath())) fileService.deleteFile(board.getConvertedVideoPath());
        if (StringUtils.hasText(board.getThumbnailPath())) fileService.deleteFile(board.getThumbnailPath());
    }

    private boolean isOwnerOrAdmin(Board board, Long userId) {
        return userId != null && (board.isAuthor(userId) || isAdmin(userId));
    }

    private boolean isAdmin(Long userId) {
        return userId != null && users.findById(userId).map(u -> u.getRoles().contains("ROLE_ADMIN")).orElse(false);
    }

    private String buildPublicUrl(String relativePath) {
        if (!StringUtils.hasText(relativePath)) return null;
        return convertedBaseUrl + (relativePath.startsWith("/") ? relativePath : "/" + relativePath);
    }

    private void sendEmail(User author, String boardTitle, Long boardId, boolean isSuccess) {
        String subject = isSuccess ? "MemoryLab 동영상 변환이 완료되었습니다!" : "MemoryLab 동영상 변환에 실패했습니다.";
        String body = String.format(isSuccess ?
                "안녕하세요, %s님!\n\n게시글 '%s'의 동영상 변환이 완료되었습니다.\n지금 바로 확인해보세요!\n%s" :
                "안녕하세요, %s님.\n\n아쉽게도 게시글 '%s'의 동영상 변환에 실패했습니다.\n다시 시도하거나 관리자에게 문의해주세요.",
                author.getNickname(), boardTitle, "http://your-service-domain.com/board/view?id=" + boardId);
        mail.send(author.getEmail(), subject, body);
    }

    private String nz(String s) { return (s == null) ? "" : s.trim(); }
}
