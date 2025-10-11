# 🏛️ MemoryLab (추억현상소)

> **사라져가는 공간을 디지털 3D 재구성으로 기록하고 공유하는 웹 아카이빙 플랫폼**

MemoryLab은 사용자가 업로드한 동영상을 **3D Gaussian Splatting** 기술을 활용하여 3D 모델로 변환하고, 이를 웹 브라우저에서 실시간으로 탐색할 수 있게 하는 플랫폼입니다. 사라져가는 장소, 추억이 담긴 물건 등을 디지털 형태로 보존하고 공유하는 것을 목표로 합니다.

---

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [핵심 구현 사항](#-핵심-구현-사항)
- [데이터베이스 설계](#-데이터베이스-설계)
- [API 설계](#-api-설계)
- [개발 환경 설정](#-개발-환경-설정)
- [배포 아키텍처](#-배포-아키텍처)
- [트러블슈팅](#-트러블슈팅)

---

## 🎯 프로젝트 개요

### 개발 기간
- **2025.08 ~ 2025.12** (예정)

### 프로젝트 목표
1. **디지털 아카이빙**: 사라져가는 공간과 물건을 3D로 디지털화하여 영구 보존
2. **기술 통합**: AI 기반 3D 재구성 기술과 웹 기술의 통합
3. **사용자 경험**: 복잡한 3D 변환 과정을 사용자 친화적인 웹 인터페이스로 제공
4. **확장성**: 대용량 파일 처리와 비동기 작업 처리를 통한 높은 처리량 달성

### 프로젝트 구조
```
memorylab/
├── memorylab/              # Spring Boot 백엔드
│   ├── src/main/java/com/memorylab/
│   │   ├── ai/            # AI 서버 통합 (WebClient, Webhook)
│   │   ├── config/        # 보안, JWT, WebClient, ShedLock 설정
│   │   ├── controller/    # REST API 컨트롤러
│   │   ├── domain/        # JPA 엔티티 (Board, Member, Comment 등)
│   │   ├── dto/           # 데이터 전송 객체
│   │   ├── repository/    # Spring Data JPA 리포지토리
│   │   ├── scheduler/     # 스케줄링 작업 (상태 폴링, 썸네일 생성 등)
│   │   └── service/       # 비즈니스 로직
│   └── src/main/resources/
│       ├── application.yml           # 기본 설정
│       └── application-prod.yml      # 운영 환경 설정
│
└── memories_lab/           # React + TypeScript 프론트엔드
    ├── src/
    │   ├── components/    # 재사용 가능한 UI 컴포넌트
    │   │   ├── Post/      # 게시판 관련 컴포넌트
    │   │   │   ├── BoardList.tsx          # 게시글 목록 (카드 뷰)
    │   │   │   ├── BoardDetailContent.tsx # 게시글 상세 페이지
    │   │   │   ├── BoardCreateContent.tsx # 게시글 작성 (파일 업로드)
    │   │   │   └── PlyViewer.tsx          # 3D PLY 뷰어
    │   │   └── main/      # 메인 페이지 컴포넌트
    │   ├── contexts/      # React Context (AuthContext)
    │   ├── hooks/         # 커스텀 훅 (useAuth, useMobile 등)
    │   ├── pages/         # 페이지 컴포넌트
    │   ├── services/      # API 클라이언트
    │   │   ├── api.ts             # HTTP 클라이언트 (JWT 자동 갱신)
    │   │   └── endpoints.ts       # 타입 안전한 API 엔드포인트
    │   └── types/         # TypeScript 타입 정의
    └── vite.config.dev.ts # Vite 개발 서버 설정 (API 프록시)
```

---

## ✨ 주요 기능

### 1. 사용자 인증 및 권한 관리

#### JWT 기반 인증 시스템
- **Access Token + Refresh Token** 구조로 보안 강화
- Access Token 만료 시 Refresh Token으로 자동 갱신 (프론트엔드에서 처리)
- Spring Security와 JWT 필터를 통한 요청별 인증 검증

```java
// JwtAuthenticationFilter.java - 모든 요청에서 JWT 검증
@Override
protected void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) {
    String token = extractTokenFromHeader(request);
    if (token != null && jwtProvider.validateToken(token)) {
        Authentication auth = jwtProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
}
```

```typescript
// api.ts - 프론트엔드에서 401 응답 시 자동 토큰 갱신
if (response.status === 401 && !skipAuth) {
    const newToken = await this.refreshAccessToken();
    if (newToken) {
        // 새 토큰으로 원래 요청 재시도
        headers['Authorization'] = `Bearer ${newToken}`;
        response = await fetch(url, { ...restConfig, headers });
    }
}
```

#### 이메일 인증 시스템
- **Spring Mail**과 Gmail SMTP를 통한 인증 코드 발송
- 6자리 랜덤 인증 코드 (5분 유효)
- 비동기 이메일 발송 (`@Async`)으로 사용자 경험 개선

```java
// EmailService.java
@Async
public void sendVerificationCode(String email, String code) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setSubject("[MemoryLab] 이메일 인증 코드");
    message.setText("인증 코드: " + code + "\n(5분 내에 입력해주세요)");
    mailSender.send(message);
}
```

#### 역할 기반 접근 제어 (RBAC)
- `ROLE_USER`: 일반 사용자 (게시글 작성, 댓글, 좋아요)
- `ROLE_ADMIN`: 관리자 (모든 게시글 수정/삭제, 공지사항 작성)

### 2. 게시판 시스템

#### 게시글 작성 및 파일 업로드
- **대용량 파일 지원**: 최대 500MB MP4 파일 업로드
- **드래그 앤 드롭** 파일 업로드 UI
- **XMLHttpRequest 기반 업로드 진행률 추적**

```typescript
// BoardCreateContent.tsx - 실시간 업로드 진행률 표시
const xhr = new XMLHttpRequest();
xhr.upload.addEventListener('progress', (event) => {
    if (event.lengthComputable) {
        const progress = Math.round((event.loaded / event.total) * 100);
        onProgress?.(progress);
    }
});
```

```java
// BoardController.java - 멀티파트 파일 처리
@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<BoardDetailResponseDto> createBoard(
    @RequestPart("data") @Valid BoardCreateRequestDto dto,
    @RequestPart(value = "file", required = false) MultipartFile file,
    @AuthenticationPrincipal UserDetails userDetails
) {
    Board board = boardService.createBoard(dto, file, userDetails.getUsername());
    return ResponseEntity.ok(BoardDetailResponseDto.from(board));
}
```

#### 게시글 분류 및 공개 범위
- **카테고리**: 장면(SCENE), 물체(OBJECT), 공지사항(NOTICE), 문의(INQUIRY)
- **공개 범위**: 전체공개(PUBLIC), 비공개(PRIVATE)
- Spring Data JPA 쿼리 메서드를 통한 동적 필터링

```java
// BoardRepository.java
@Query("SELECT b FROM Board b WHERE " +
       "(:category IS NULL OR b.category = :category) AND " +
       "(:visibility IS NULL OR b.visibility = :visibility) AND " +
       "(b.visibility = 'PUBLIC' OR b.user.id = :userId)")
Page<Board> findFilteredBoards(
    @Param("category") Category category,
    @Param("visibility") Visibility visibility,
    @Param("userId") Long userId,
    Pageable pageable
);
```

#### 페이지네이션 및 검색
- **Spring Data JPA Pageable** 기반 페이지네이션
- 제목/내용 검색 (LIKE 쿼리)
- 정렬 옵션: 최신순, 인기순(조회수), 좋아요순

#### 게시글 상호작용
- **조회수**: 게시글 열람 시 자동 증가 (낙관적 락 방지)
- **좋아요**: 토글 방식 (중복 좋아요 방지, 복합 키 사용)
- **댓글**: CRUD 기능, 작성자만 수정/삭제 가능

```java
// Board.java - 낙관적 락을 통한 동시성 제어
@Version
private Long version;

public void increaseViewCount() {
    this.viewCount++;
}
```

```java
// BoardLike.java - 복합 키를 통한 중복 좋아요 방지
@EmbeddedId
private BoardLikeId id; // (boardId, userId)
```

### 3. 3D 모델 변환 시스템

#### AI 서버 통합 아키텍처

MemoryLab의 핵심 기능인 3D 변환은 **외부 AI 서버**(GPU 서버)와의 통합을 통해 이루어집니다. 백엔드는 AI 서버와의 통신을 관리하며, **Webhook 콜백 + 폴링 백업** 전략을 통해 높은 신뢰성을 보장합니다.

```
[사용자] → [프론트엔드] → [백엔드] → [AI 서버]
                              ↑          ↓
                              └─ Webhook ─┘
                              ↑          ↓
                              └─ Polling ─┘ (60초 간격, 백업용)
```

#### 3D 변환 플로우

##### 1단계: 파일 업로드 및 AI 작업 요청

사용자가 MP4 파일을 업로드하면, 백엔드는 파일을 로컬에 저장하고 AI 서버에 변환 작업을 요청합니다.

```java
// BoardService.java
public Board createBoard(BoardCreateRequestDto dto, MultipartFile file, String email) {
    // 1. 파일 저장 (UUID 파일명)
    String videoPath = fileService.saveFile(file, FileType.VIDEO);

    // 2. Board 엔티티 생성
    Board board = Board.builder()
        .user(user)
        .title(dto.getTitle())
        .content(dto.getContent())
        .category(dto.getCategory())
        .visibility(dto.getVisibility())
        .originalVideoPath(videoPath)
        .build();
    board.setThumbnailStatus(ThumbnailStatus.PENDING); // 썸네일 생성 대기
    boardRepository.save(board);

    // 3. AI 서버에 변환 요청 (비동기)
    String publicUrl = "https://mlab.snowytiger.me/uploads/videos/" + filename;
    aiServerClient.requestUpload(board, publicUrl);

    return board;
}
```

```java
// AiServerClient.java - WebClient 기반 비동기 HTTP 통신
public void requestUpload(Board board, String fileUrl) {
    AiUploadRequestDto requestDto = new AiUploadRequestDto(filename, fileUrl);

    webClient.post()
        .uri(aiServerBaseUrl + "/upload")
        .bodyValue(requestDto)
        .retrieve()
        .onStatus(HttpStatus.SERVICE_UNAVAILABLE::equals, response -> {
            // 503: AI 서버 대기열 가득 참 → 재시도 카운트 증가
            log.warn("AI 서버 대기열 꽉 참 (503): boardId={}", boardId);
            increaseRetryCountWithRetry(boardId, true);
            return Mono.error(new RuntimeException("AI 서버 대기열 꽉 참"));
        })
        .bodyToMono(AiTaskResponseDto.class)
        .subscribe(taskResponse -> {
            // taskId 수신 → Board에 저장 (ExternalStatus.QUEUED)
            updateBoardWithRetry(boardId, taskResponse.getTask_id(),
                                 ExternalStatus.QUEUED, false);
        });
}
```

**AI 서버 응답 (202 Accepted)**:
```json
{
  "task_id": "abc123-xyz789",
  "status": "QUEUED",
  "message": "Task queued for processing"
}
```

##### 2단계: AI 서버 처리 및 상태 업데이트

AI 서버는 3D Gaussian Splatting 알고리즘을 실행하여 동영상을 PLY 파일로 변환합니다. 처리 상태는 두 가지 방식으로 백엔드에 전달됩니다.

**방법 1: Webhook 콜백 (Primary)**

AI 서버가 작업 완료/실패 시 백엔드의 Webhook 엔드포인트를 호출합니다.

```java
// AiCallbackController.java
@PostMapping("/api/v1/ai-callback/notify")
public ResponseEntity<Void> handleAiCallback(
    @RequestHeader("X-Signature") String signature,
    @RequestBody AiNotificationDto payload
) {
    // 1. Webhook 서명 검증 (보안)
    if (!signatureValidator.isValid(payload, signature)) {
        log.error("Webhook 서명 검증 실패");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // 2. 상태에 따라 처리
    switch (payload.getStatus()) {
        case "COMPLETED":
            aiResultHandlerService.queuePlyDownload(
                payload.getTaskId(),
                payload.getResultUrl()
            );
            break;
        case "FAILED":
            aiResultHandlerService.processFailedTask(
                payload.getTaskId(),
                payload.getErrorCode(),
                payload.getErrorDetail()
            );
            break;
    }

    return ResponseEntity.ok().build();
}
```

**Webhook Payload 예시 (COMPLETED)**:
```json
{
  "task_id": "abc123-xyz789",
  "status": "COMPLETED",
  "result_url": "https://ai-server.com/results/abc123.ply",
  "timestamp": "2025-10-11T12:34:56Z"
}
```

**방법 2: 폴링 (Fallback)**

Webhook이 실패할 경우를 대비하여 **60초마다** AI 서버의 상태 API를 폴링합니다.

```java
// AiStatusScheduler.java
@Scheduled(fixedDelay = 60000) // 60초 간격
public void pollAiServerStatus() {
    // QUEUED 또는 PROCESSING 상태인 게시글 조회
    List<Board> boardsToCheck = boardRepository.findByExternalStatusIn(
        List.of(ExternalStatus.QUEUED, ExternalStatus.PROCESSING)
    );

    boardsToCheck.forEach(board -> {
        webClient.get()
            .uri(aiServerBaseUrl + "/task?task_id=" + board.getAiTaskId())
            .retrieve()
            .bodyToMono(AiStatusInfo.class)
            .subscribe(statusInfo -> {
                switch (statusInfo.getStatus()) {
                    case "COMPLETED":
                        aiResultHandlerService.queuePlyDownload(
                            board.getAiTaskId(),
                            statusInfo.getTask().getResultUrl()
                        );
                        break;
                    case "FAILED":
                        board.setExternalStatus(ExternalStatus.FAILED);
                        board.setStatus(BoardStatus.FAILED_PROCESS);
                        break;
                }
            });
    });
}
```

##### 3단계: PLY 파일 다운로드

AI 서버로부터 PLY 파일 다운로드 URL을 받으면, 백엔드는 파일을 다운로드하여 로컬에 저장합니다.

```java
// FileDownloadService.java
@Async
public void downloadPlyFile(Board board, String resultUrl) {
    try {
        board.setStatus(BoardStatus.DOWNLOADING);
        boardRepository.save(board);

        // WebClient로 파일 다운로드 (스트리밍)
        Mono<byte[]> plyDataMono = webClient.get()
            .uri(resultUrl)
            .retrieve()
            .bodyToMono(byte[].class);

        byte[] plyData = plyDataMono.block();

        // 로컬 파일 시스템에 저장
        String plyPath = "/srv/memorylab/uploads/ply/" + board.getId() + ".ply";
        Files.write(Path.of(plyPath), plyData);

        // Board 상태 업데이트
        board.setPlyPath(plyPath);
        board.setStatus(BoardStatus.READY); // ✅ 3D 모델 준비 완료
        board.setExternalStatus(ExternalStatus.COMPLETED);
        boardRepository.save(board);

        log.info("PLY 파일 다운로드 완료: boardId={}", board.getId());

    } catch (Exception e) {
        log.error("PLY 다운로드 실패: boardId={}", board.getId(), e);
        board.setStatus(BoardStatus.FAILED_DOWNLOAD);
        boardRepository.save(board);
    }
}
```

##### 4단계: 프론트엔드에서 3D 렌더링

게시글 상태가 `READY`가 되면, 프론트엔드는 PLY 파일을 로드하여 **3D Gaussian Splatting** 뷰어로 렌더링합니다.

```typescript
// PlyViewer.tsx - Gaussian Splats 3D 라이브러리 사용
import * as GaussianSplats3D from '@mkkellogg/gaussian-splats-3d';

useEffect(() => {
    const plyUrl = `https://mlab.snowytiger.me${normalizedPath}`;

    // Three.js 씬 생성
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000);
    const renderer = new THREE.WebGLRenderer({ antialias: true });

    // Gaussian Splat Viewer 초기화
    const viewer = new GaussianSplats3D.Viewer({
        renderer, camera, scene,
        sphericalHarmonicsDegree: 2,
    });

    // PLY 파일 로드 (프로그레시브 로딩)
    viewer.addSplatScene(plyUrl, {
        progressiveLoad: true,
        onProgress: (percent) => setProgress(Math.round(percent * 100)),
    })
    .then(() => {
        viewer.start(); // 렌더링 시작
    })
    .catch((err) => setError('3D 모델 로딩 실패'));
}, [plyPath]);
```

#### 다중 상태 관리 시스템

`Board` 엔티티는 복잡한 비동기 작업을 추적하기 위해 **여러 개의 상태 필드**를 관리합니다.

```java
// Board.java
public class Board {
    // 1. 내부 처리 상태 (사용자에게 노출되는 상태)
    @Enumerated(EnumType.STRING)
    private BoardStatus status;
    // - DISPATCHED: AI 서버에 요청 발송
    // - PROCESSING: AI 서버에서 처리 중
    // - RESULT_READY: 처리 완료, 결과 파일 준비됨
    // - DOWNLOADING: PLY 파일 다운로드 중
    // - READY: 3D 모델 사용 가능 ✅
    // - FAILED_PROCESS: AI 처리 실패
    // - FAILED_DOWNLOAD: PLY 다운로드 실패

    // 2. AI 서버 상태 (외부 시스템 상태)
    @Enumerated(EnumType.STRING)
    private ExternalStatus externalStatus;
    // - QUEUED: AI 서버 대기열에 추가됨
    // - PROCESSING: AI 서버에서 처리 중
    // - COMPLETED: AI 처리 완료
    // - FAILED: AI 처리 실패

    // 3. 썸네일 생성 상태
    @Enumerated(EnumType.STRING)
    private ThumbnailStatus thumbnailStatus;
    // - NONE: 썸네일 생성 전
    // - PENDING: 썸네일 생성 대기
    // - READY: 썸네일 생성 완료
    // - FAILED: 썸네일 생성 실패

    // 4. 비디오 트랜스코딩 상태 (현재는 미사용)
    @Enumerated(EnumType.STRING)
    private TranscodeStatus transcodeStatus;
}
```

**상태 매핑 로직** (`BoardService.mapToBoardStatus()`):
```java
private BoardStatus mapToBoardStatus(Board board) {
    ExternalStatus ext = board.getExternalStatus();

    // AI 서버 상태가 없으면 → DISPATCHED (초기 상태)
    if (ext == null) return BoardStatus.DISPATCHED;

    switch (ext) {
        case QUEUED:
        case PROCESSING:
            return BoardStatus.PROCESSING;
        case COMPLETED:
            // PLY 파일이 있으면 READY, 없으면 DOWNLOADING
            return board.getPlyPath() != null
                ? BoardStatus.READY
                : BoardStatus.DOWNLOADING;
        case FAILED:
            return BoardStatus.FAILED_PROCESS;
        default:
            return BoardStatus.DISPATCHED;
    }
}
```

#### 동시성 제어: 낙관적 락 (Optimistic Locking)

여러 스레드(Webhook, 폴링, 썸네일 생성 등)가 동시에 `Board` 엔티티를 수정할 수 있기 때문에, **JPA의 낙관적 락**을 사용하여 동시성 문제를 방지합니다.

```java
// Board.java
@Version
private Long version; // JPA가 자동으로 버전 관리

// AiServerClient.java - 낙관적 락 충돌 시 재시도
@Transactional
public void updateBoardWithRetry(Long boardId, String taskId,
                                   ExternalStatus status, Boolean isQueueFull) {
    int maxRetries = 3;
    for (int attempt = 0; attempt < maxRetries; attempt++) {
        try {
            Board board = boardRepository.findById(boardId).orElseThrow();
            board.setAiTaskId(taskId);
            board.setExternalStatus(status);
            boardRepository.saveAndFlush(board);
            return; // 성공
        } catch (ObjectOptimisticLockingFailureException e) {
            if (attempt == maxRetries - 1) {
                throw new RuntimeException("낙관적 락 재시도 실패", e);
            }
            Thread.sleep(50 * (attempt + 1)); // 백오프
        }
    }
}
```

#### 에러 처리 및 재시도 전략

##### AI 서버 대기열 가득 참 (503)
```java
// AI 서버가 503 응답을 반환하면
.onStatus(HttpStatus.SERVICE_UNAVAILABLE::equals, response -> {
    board.setQueueFull(true); // 플래그 설정
    board.increaseRetryCount(); // 재시도 횟수 증가
    // AiUploadRetryScheduler가 주기적으로 재시도
})
```

```java
// AiUploadRetryScheduler.java - 매 5분마다 재시도
@Scheduled(fixedDelay = 300000) // 5분
public void retryFailedUploads() {
    List<Board> failedBoards = boardRepository.findByIsQueueFullTrue();

    for (Board board : failedBoards) {
        if (board.getRetryCount() < 10) { // 최대 10회 재시도
            aiServerClient.requestUpload(board, videoUrl);
        } else {
            board.setStatus(BoardStatus.FAILED_PROCESS);
            board.setExternalErrorCode("MAX_RETRIES_EXCEEDED");
        }
    }
}
```

##### AI 처리 실패 (FAILED)
```java
// AiResultHandlerService.java
public void processFailedTask(String taskId, String errorCode, String errorDetail) {
    board.setExternalStatus(ExternalStatus.FAILED);
    board.setExternalErrorCode(errorCode);
    board.setExternalErrorDetail(errorDetail);
    board.setStatus(BoardStatus.FAILED_PROCESS);

    // 사용자에게 실패 이메일 발송
    sendFailureEmail(board, errorCode, errorDetail);
}
```

**사용자 친화적인 에러 메시지 매핑**:
```java
// ErrorMessageMapper.java
public static String getErrorMessage(String errorCode, String errorDetail) {
    return switch (errorCode) {
        case "INSUFFICIENT_FRAMES" ->
            "동영상의 프레임 수가 부족합니다. 최소 30초 이상의 동영상을 업로드해주세요.";
        case "RECONSTRUCTION_FAILED" ->
            "3D 재구성에 실패했습니다. 다양한 각도에서 촬영한 동영상을 사용해주세요.";
        case "FILE_CORRUPTED" ->
            "파일이 손상되었습니다. 다른 파일을 업로드해주세요.";
        default ->
            "처리 중 오류가 발생했습니다: " + errorDetail;
    };
}
```

##### 성공 시 이메일 알림
```java
// AiResultHandlerService.java
private void sendSuccessEmail(Board board) {
    String body = String.format(
        "안녕하세요, %s님!\n\n" +
        "업로드하신 게시글 '%s'의 3D 모델 변환이 성공적으로 완료되었습니다.\n\n" +
        "게시글 보기: https://mlab.snowytiger.me/post/%d",
        board.getUser().getNickname(),
        board.getTitle(),
        board.getId()
    );
    emailService.send(board.getUser().getEmail(),
                      "[MemoryLab] 3D 모델 변환 완료", body);
}
```

### 4. 썸네일 자동 생성

동영상 업로드 시 **FFmpeg**를 사용하여 썸네일을 자동 생성합니다.

```java
// ThumbnailScheduler.java
@Scheduled(fixedDelay = 30000) // 30초마다 실행
@SchedulerLock(name = "ThumbnailScheduler", lockAtMostFor = "10m")
public void generatePendingThumbnails() {
    List<Board> pendingBoards = boardRepository
        .findByThumbnailStatus(ThumbnailStatus.PENDING);

    for (Board board : pendingBoards) {
        thumbnailService.generateThumbnail(board);
    }
}
```

```java
// ThumbnailService.java - FFmpeg 프로세스 실행
public void generateThumbnail(Board board) {
    String inputPath = board.getOriginalVideoPath();
    String outputPath = "/srv/memorylab/thumbnails/" + board.getId() + ".jpg";

    // FFmpeg 명령어: 1초 시점에서 640x360 썸네일 생성
    ProcessBuilder pb = new ProcessBuilder(
        "ffmpeg", "-i", inputPath, "-ss", "00:00:01",
        "-vframes", "1", "-vf", "scale=640:360",
        "-y", outputPath
    );

    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode == 0) {
        board.setThumbnailPath(outputPath);
        board.setThumbnailStatus(ThumbnailStatus.READY);
    } else {
        board.setThumbnailStatus(ThumbnailStatus.FAILED);
    }

    boardRepository.save(board);
}
```

### 5. 반응형 디자인 및 UX

#### 모바일/태블릿/데스크톱 대응
```css
/* 모바일 (≤768px) */
@media (max-width: 768px) {
    .boardList {
        grid-template-columns: 1fr;
    }
}

/* 태블릿 (769px~1023px) */
@media (min-width: 769px) and (max-width: 1023px) {
    .boardList {
        grid-template-columns: repeat(2, 1fr);
    }
}

/* 데스크톱 (≥1024px) */
@media (min-width: 1024px) {
    .boardList {
        grid-template-columns: repeat(3, 1fr);
    }
}
```

#### Framer Motion 애니메이션
```typescript
// BoardList.tsx - 카드 등장 애니메이션
<motion.div
    initial={{ opacity: 0, y: 20 }}
    animate={{ opacity: 1, y: 0 }}
    transition={{ duration: 0.3, delay: index * 0.1 }}
>
    <BoardCard board={board} />
</motion.div>
```

### 6. 관리자 기능

#### 게시글 관리
- 모든 게시글 수정/삭제 권한
- AI 작업 수동 재시도
- 썸네일 재생성

```java
// AdminBoardController.java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/{boardId}/retry-ai")
public ResponseEntity<Void> retryAiProcessing(@PathVariable Long boardId) {
    boardAdminService.retryAiProcessing(boardId);
    return ResponseEntity.ok().build();
}
```

---

## 🛠️ 기술 스택

### Backend

#### Core Framework
- **Spring Boot 3.3.1** (Java 21)
  - Spring MVC: RESTful API 구현
  - Spring Data JPA: ORM 및 데이터베이스 접근
  - Spring Security: 인증 및 권한 관리
  - Spring WebFlux: 비동기 HTTP 클라이언트 (AI 서버 통신)
  - Spring Mail: 이메일 발송
  - Spring AOP: 횡단 관심사 처리

#### Security
- **Spring Security**
  - JWT 기반 stateless 인증
  - Role-based access control (RBAC)
  - CORS 설정
- **io.jsonwebtoken (JJWT) 0.12.5**
  - JWT 생성 및 검증
  - HS512 서명 알고리즘

#### Database
- **MySQL 8.0**
  - 프로덕션 데이터베이스
  - 인덱싱: `aiTaskId` (UNIQUE), `user_id`, `category`, `visibility`
- **Hikari CP**
  - 커넥션 풀 관리

#### HTTP Client
- **Spring WebFlux (WebClient)**
  - Non-blocking 비동기 HTTP 통신
  - AI 서버와의 통신 (업로드 요청, 상태 조회, 리소스 삭제)
  - Reactive Streams 기반

```java
// WebClientConfig.java
@Bean
public WebClient webClient(WebClient.Builder builder) {
    return builder
        .baseUrl(aiServerBaseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
}
```

#### Resilience & Fault Tolerance
- **Resilience4j**
  - Circuit Breaker: AI 서버 장애 시 연쇄 장애 방지
  - Retry: 일시적 네트워크 오류 자동 재시도
  - Bulkhead: 동시 요청 수 제한

#### Scheduling
- **Spring Scheduler** (`@Scheduled`)
  - 썸네일 생성 (30초 간격)
  - AI 상태 폴링 (60초 간격)
  - 재시도 큐 처리 (5분 간격)
  - 오래된 파일 정리 (매일 03:00)
- **ShedLock 5.10.2**
  - 분산 환경에서 스케줄러 중복 실행 방지
  - JDBC 기반 락 관리

#### Logging
- **SLF4J + Logback**
  - JSON 형식 로그 출력 (프로덕션)
  - MDC (Mapped Diagnostic Context)를 통한 요청별 추적 ID

#### File Processing
- **FFmpeg**
  - 동영상 썸네일 생성 (1초 시점, 640x360)
  - 비디오 트랜스코딩 (향후 구현 예정)

#### Utilities
- **Lombok**
  - 보일러플레이트 코드 제거 (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`)
- **Spring Boot DevTools**
  - 자동 재시작, 라이브 리로드 (개발 환경)

### Frontend

#### Core Framework
- **React 18.2**
  - Hooks 기반 함수형 컴포넌트
  - Context API (AuthContext)
- **TypeScript 5.2**
  - 타입 안전성 보장
  - API 응답 타입 정의 (`types/api.ts`)

#### Build Tool
- **Vite 5.0**
  - 초고속 HMR (Hot Module Replacement)
  - ES Modules 기반 빌드
  - 개발 서버 API 프록시 (`/api` → `http://localhost:8080`)

#### Routing
- **React Router 6.20**
  - 클라이언트 사이드 라우팅
  - Protected Routes (AuthContext 기반)

#### 3D Rendering
- **@mkkellogg/gaussian-splats-3d 0.4.7**
  - 3D Gaussian Splatting PLY 파일 렌더링
  - Progressive loading (점진적 로딩)
- **Three.js 0.180**
  - WebGL 기반 3D 그래픽스
  - 씬, 카메라, 렌더러 관리
- **@react-three/fiber 9.3**
  - React용 Three.js 래퍼
- **@react-three/drei 10.7**
  - Three.js 유틸리티 컴포넌트

#### UI Components
- **Radix UI**
  - `@radix-ui/react-dialog`: 모달
  - `@radix-ui/react-navigation-menu`: 네비게이션
  - `@radix-ui/react-scroll-area`: 스크롤 영역
  - `@radix-ui/react-select`: 드롭다운
- **Lucide React**
  - 아이콘 라이브러리

#### Animation
- **Framer Motion 10.16**
  - 선언적 애니메이션
  - 페이지 전환, 카드 등장 효과

#### HTTP Client
- **Fetch API**
  - 커스텀 `ApiClient` 클래스 (`services/api.ts`)
  - JWT 자동 갱신
  - 에러 핸들링

#### Styling
- **CSS Modules**
  - 컴포넌트별 스타일 격리
  - 클래스명 충돌 방지

---

## 🏗️ 시스템 아키텍처

### 전체 아키텍처 다이어그램

```
┌─────────────┐
│   사용자    │
└──────┬──────┘
       │ HTTPS
       ▼
┌──────────────────────────────────────────────┐
│              Nginx (Reverse Proxy)            │
│  - React SPA (/)                              │
│  - API Proxy (/api → :8080)                   │
│  - Static Files (/uploads, /thumbnails)       │
└──────┬──────────────────────┬────────────────┘
       │                      │
       │ /                    │ /api
       ▼                      ▼
┌─────────────┐        ┌──────────────────────┐
│   React     │        │   Spring Boot        │
│  Frontend   │◄───────┤   Backend            │
│  (Vite)     │  JWT   │   - REST API         │
│             │        │   - JWT Auth         │
│             │        │   - JPA/Hibernate    │
└─────────────┘        └──────┬───────────────┘
                              │
                              │ JPA
                              ▼
                       ┌──────────────┐
                       │    MySQL     │
                       │   Database   │
                       └──────────────┘
                              ▲
                              │
┌─────────────────────────────┘
│
│  비동기 작업 (Scheduled Tasks)
│
├─ AiStatusScheduler (60초 간격)
│  └─ AI 서버 상태 폴링
│
├─ ThumbnailScheduler (30초 간격)
│  └─ FFmpeg로 썸네일 생성
│
├─ AiUploadRetryScheduler (5분 간격)
│  └─ 실패한 AI 업로드 재시도
│
└─ CleanupScheduler (매일 03:00)
   └─ 오래된 파일 삭제

┌──────────────────────────────────────────────┐
│          External AI Server (GPU)            │
│  - 3D Gaussian Splatting                     │
│  - POST /upload (파일 URL 전송)               │
│  - GET /task?task_id={id} (상태 조회)        │
│  - POST {webhook_url}/api/v1/ai-callback     │
└──────────────────────────────────────────────┘
```

---

## 💾 데이터베이스 설계

### ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────┐
│                  Member                     │
├─────────────────────────────────────────────┤
│ id (PK)                BIGINT               │
│ email                  VARCHAR(100) UNIQUE  │
│ password               VARCHAR(255)         │
│ nickname               VARCHAR(50) UNIQUE   │
│ role                   VARCHAR(20)          │
│ isEmailVerified        BOOLEAN              │
│ createdAt              DATETIME             │
│ modifiedAt             DATETIME             │
└────────────┬────────────────────────────────┘
             │ 1
             │
             │ N
┌────────────┴────────────────────────────────┐
│                  Board                      │
├─────────────────────────────────────────────┤
│ id (PK)                BIGINT               │
│ user_id (FK)           BIGINT               │
│ title                  VARCHAR(200)         │
│ content                TEXT                 │
│ category               VARCHAR(20)          │
│ visibility             VARCHAR(20)          │
│ viewCount              INT DEFAULT 0        │
│ likeCount              INT (Formula)        │
│ commentCount           INT (Formula)        │
│ originalVideoPath      VARCHAR(512)         │
│ convertedVideoPath     VARCHAR(512)         │
│ thumbnailPath          VARCHAR(512)         │
│ plyPath                VARCHAR(512)         │
│ transcodeStatus        VARCHAR(20)          │
│ thumbnailStatus        VARCHAR(20)          │
│ status                 VARCHAR(20)          │
│ retryCount             INT DEFAULT 0        │
│ isQueueFull            BOOLEAN              │
│ aiTaskId               VARCHAR(64) UNIQUE   │
│ externalStatus         VARCHAR(20)          │
│ externalResultUrl      TEXT                 │
│ externalErrorCode      VARCHAR(64)          │
│ externalErrorDetail    TEXT                 │
│ version                BIGINT (Optimistic)  │
│ createdAt              DATETIME             │
│ modifiedAt             DATETIME             │
└────────┬───────────┬──────────────────────┬─┘
         │ 1         │ 1                    │ 1
         │           │                      │
         │ N         │ N                    │ N
┌────────┴─────┐ ┌──┴──────────┐ ┌─────────┴──────────┐
│   Comment    │ │  BoardLike  │ │   RefreshToken     │
├──────────────┤ ├─────────────┤ ├────────────────────┤
│ id (PK)      │ │ board_id(PK)│ │ id (PK)            │
│ board_id (FK)│ │ user_id (PK)│ │ token   VARCHAR(255)│
│ user_id (FK) │ │ createdAt   │ │ user_id (FK)       │
│ content      │ └─────────────┘ │ expiryDate         │
│ createdAt    │                 │ createdAt          │
│ modifiedAt   │                 └────────────────────┘
└──────────────┘

┌───────────────────────────┐
│    VerificationCode       │
├───────────────────────────┤
│ id (PK)      BIGINT       │
│ email        VARCHAR(100) │
│ code         VARCHAR(6)   │
│ expiryDate   DATETIME     │
│ isUsed       BOOLEAN      │
│ createdAt    DATETIME     │
└───────────────────────────┘
```

---

## 📝 개발 노트

### 기술적 의사결정

#### 1. AI 서버 통신: WebFlux vs RestTemplate
- **선택**: Spring WebFlux (WebClient)
- **이유**:
  - Non-blocking 비동기 처리로 스레드 효율성 향상
  - AI 서버 응답 대기 시 다른 요청 처리 가능
  - Reactive Streams를 통한 백프레셔 지원

#### 2. 상태 동기화: Webhook vs Polling
- **선택**: Hybrid (Webhook 우선, Polling 백업)
- **이유**:
  - Webhook: 즉시성 보장, 네트워크 트래픽 최소화
  - Polling: Webhook 실패 시 백업, 일관성 보장

#### 3. 동시성 제어: 낙관적 락 vs 비관적 락
- **선택**: 낙관적 락 (`@Version`)
- **이유**:
  - 읽기 작업이 쓰기 작업보다 많음
  - 충돌 가능성이 낮음
  - 데이터베이스 락 오버헤드 최소화

#### 4. 프론트엔드 빌드 도구: Vite vs Webpack
- **선택**: Vite
- **이유**:
  - 빠른 HMR (Hot Module Replacement)
  - ES Modules 네이티브 지원
  - 개발 경험 향상

---

## 🔒 보안 고려사항

### 1. 인증 및 권한
- **JWT**: Stateless 인증으로 확장성 확보
- **Refresh Token**: Access Token 만료 시 재발급으로 보안 강화
- **BCrypt**: 비밀번호 해싱 (Salt + 10 rounds)

### 2. 입력 검증
- **@Valid**: DTO에서 입력 값 검증 (Hibernate Validator)
- **파일 검증**: MIME 타입, 파일 크기, 확장자 검증

### 3. SQL Injection 방지
- **JPA Parameterized Query**: Spring Data JPA의 `@Query`로 파라미터 바인딩

### 4. CSRF 방지
- **JWT 기반 인증**: CSRF 토큰 불필요 (Stateless)

### 5. Webhook 보안
- **HMAC 서명**: AI 서버의 Webhook 요청을 HMAC-SHA256으로 검증

---

## 📈 성능 최적화

### 데이터베이스 쿼리 최적화
- **N+1 문제 해결**: `@EntityGraph`로 fetch join 사용
- **Projection**: 필요한 컬럼만 조회하여 네트워크 트래픽 감소
- **인덱싱**: 자주 조회되는 필드에 인덱스 추가

### 비동기 처리
- **@Async**: 이메일 발송, PLY 다운로드를 비동기로 처리하여 응답 시간 단축
- **WebClient**: Non-blocking HTTP 통신으로 AI 서버와 통신

---

## 🚀 개발 환경 설정

### 사전 요구사항
- **Java 21** (OpenJDK 또는 Oracle JDK)
- **Node.js 20+** (LTS 버전 권장)
- **pnpm 8+** (패키지 매니저)
- **MySQL 8.0+**
- **FFmpeg** (썸네일 생성용)
- **Git**

### 백엔드 설정

```bash
# 데이터베이스 생성
mysql -u root -p
CREATE DATABASE memorylab CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 백엔드 실행
cd memorylab
./gradlew bootRun    # Unix/macOS
./gradlew.bat bootRun  # Windows
```

### 프론트엔드 설정

```bash
# 의존성 설치
cd memories_lab
pnpm install

# 개발 서버 실행
pnpm dev
```

---

## 📧 연락처

- **GitHub**: https://github.com/yourusername/memorylab
- **웹사이트**: https://mlab.snowytiger.me

---

**MemoryLab** - 사라져가는 추억을 디지털로 영원히 보존합니다. 🏛️✨
