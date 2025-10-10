# MemoryLab (추억현상소)

<div align="center">

**사라지는 공간을 디지털 3D로 보존하는 웹 아카이빙 플랫폼**

동영상 속 공간을 AI 기반 3D 재구성 기술로 변환하여 인터랙티브하게 탐색할 수 있는 플랫폼

</div>

---

## 프로젝트 소개

MemoryLab은 **Gaussian Splatting** 기술을 활용하여 일반 동영상을 사진처럼 사실적인 3D 공간으로 변환하는 웹 플랫폼입니다.

사용자가 업로드한 MP4 동영상을 AI 서버에서 처리하여 PLY 형식의 3D 모델로 재구성하고, 웹 브라우저에서 바로 탐색할 수 있도록 제공합니다. 처리 과정은 실시간으로 추적되며, 완성된 3D 공간은 커뮤니티에 공유할 수 있습니다.

## 핵심 기능

### 1. 동영상 → 3D 변환 파이프라인
- MP4 동영상 업로드 (최대 500MB, 드래그 앤 드롭 지원)
- 실시간 업로드 진행률 표시 (XMLHttpRequest 기반)
- AI 서버와의 비동기 통신으로 3D Gaussian Splatting 처리
- 자동 썸네일 생성 및 동영상 변환

### 2. 실시간 처리 상태 추적
다단계 상태 관리 시스템:
- **TranscodeStatus**: 동영상 변환 상태 (NONE → PENDING → CONVERTING → READY/FAILED)
- **ThumbnailStatus**: 썸네일 생성 상태 (NONE → PENDING → READY/FAILED)
- **ExternalStatus**: AI 서버 작업 상태 (QUEUED → PROCESSING → COMPLETED/FAILED)
- **BoardStatus**: 사용자에게 표시되는 통합 상태 (DISPATCHED → PROCESSING → READY/FAILED)

### 3. 인터랙티브 3D 뷰어
- `@mkkellogg/gaussian-splats-3d` 라이브러리를 활용한 WebGL 기반 렌더링
- React Three Fiber로 구현된 인터랙티브 3D 공간 탐색
- 브라우저에서 직접 PLY 파일 로드 및 실시간 렌더링

### 4. 커뮤니티 기능
- 게시판 형식의 콘텐츠 공유 (카드 스타일 UI)
- 카테고리별 필터링 및 페이지네이션
- 좋아요, 댓글 기능
- 공개/비공개 설정 (작성자 및 관리자만 비공개 게시글 열람 가능)
- 조회수 추적

### 5. 인증 및 보안
- JWT 기반 인증 (Access Token + Refresh Token)
- 자동 토큰 갱신 (401 에러 시 리프레시 토큰으로 재시도)
- 이메일 인증 시스템
- 역할 기반 접근 제어 (ROLE_USER, ROLE_ADMIN)
- AI 서버 웹훅 서명 검증

## 기술 스택

### Frontend
| 기술 | 버전 | 용도 |
|------|------|------|
| React | 18.2 | UI 프레임워크 |
| TypeScript | 5.2 | 타입 안전성 |
| Vite | 5.0 | 빌드 도구 |
| React Router | 6.20 | 클라이언트 사이드 라우팅 |
| Framer Motion | 10.16 | 애니메이션 |
| Radix UI | - | 접근성 높은 UI 컴포넌트 |
| @mkkellogg/gaussian-splats-3d | 0.4.7 | 3D Gaussian Splatting 렌더러 |
| React Three Fiber | 9.3 | Three.js React 바인딩 |
| Lucide React | - | 아이콘 라이브러리 |

### Backend
| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 21 | 프로그래밍 언어 |
| Spring Boot | 3.3.1 | 애플리케이션 프레임워크 |
| Spring Security | 6.x | 인증/인가 |
| Spring Data JPA | - | ORM 및 데이터 접근 |
| MySQL | - | 관계형 데이터베이스 |
| Spring WebFlux | - | 리액티브 HTTP 클라이언트 (AI 서버 연동) |
| Resilience4j | - | Circuit Breaker 패턴 |
| ShedLock | 5.10.2 | 분산 스케줄링 |
| JWT (jjwt) | 0.12.5 | 토큰 기반 인증 |
| Lombok | - | 보일러플레이트 감소 |

## 아키텍처

### 시스템 구조
```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│  React Frontend │ ◄─────► │ Spring Backend  │ ◄─────► │   AI Server     │
│   (Vite + TS)   │  REST   │  (Java 21 + JPA)│ WebFlux │ (3D 재구성)     │
└─────────────────┘  API    └─────────────────┘         └─────────────────┘
                                      │
                                      ▼
                             ┌─────────────────┐
                             │   MySQL DB      │
                             │  (User, Board)  │
                             └─────────────────┘
```

### AI 처리 워크플로우

```
1. 사용자 동영상 업로드
   └─► BoardService.createBoard() - 파일 저장 및 Board 엔티티 생성

2. AI 서버 전송
   └─► AiServerClient.requestUpload() - 동영상 URL을 AI 서버로 전송
   └─► AI 서버 응답: taskId 발급 → ExternalStatus.QUEUED

3. AI 서버에서 3D 재구성 처리
   └─► Gaussian Splatting 알고리즘 적용

4. 상태 업데이트 (Hybrid 방식)
   ├─► 주요: Webhook 콜백 (AiResultHandlerService)
   │   └─► AI 서버가 완료 시 POST 요청 → 서명 검증 → 상태 업데이트
   └─► 대체: 폴링 (AiStatusScheduler)
       └─► 60초마다 QUEUED/PROCESSING 상태의 작업 확인

5. PLY 파일 다운로드
   └─► externalResultUrl에서 완성된 3D 모델 다운로드

6. 3D 뷰어 렌더링
   └─► PlyViewer 컴포넌트에서 React Three Fiber로 렌더링
```

### 백엔드 주요 컴포넌트

#### 도메인 모델
- **Board** (`com.memorylab.domain.board.Board`): 핵심 엔티티
  - 파일 경로: `originalVideoPath`, `convertedVideoPath`, `thumbnailPath`, `plyPath`
  - 상태 필드: `transcodeStatus`, `thumbnailStatus`, `status`, `externalStatus`
  - AI 연동: `aiTaskId`, `externalResultUrl`, `externalErrorCode`
  - JPA `@Formula`로 동적 집계: `likeCount`, `commentCount`

#### 서비스 계층
- **BoardService**: 게시판 CRUD 및 `mapToBoardStatus()` - 다중 상태를 사용자 친화적 상태로 변환
- **AiServerClient**: WebClient 기반 AI 서버 통신
  - `requestUpload()`: 3D 변환 요청
  - `deleteAiResource()`: AI 서버 리소스 정리
- **AiResultHandlerService**: 웹훅 콜백 처리
  - `queuePlyDownload()`: 성공 처리
  - `processFailedTask()`: 실패 처리

#### 스케줄러
- **ThumbnailScheduler**: `ThumbnailStatus.PENDING` 상태의 동영상에서 썸네일 생성
- **AiStatusScheduler**: 60초마다 `QUEUED`/`PROCESSING` 상태의 작업 폴링
- **CleanupScheduler**: 오래되거나 버려진 리소스 정리

### 프론트엔드 주요 컴포넌트

#### 서비스 레이어 (타입 안전 API 클라이언트)
- **`services/api.ts`**:
  - JWT 토큰 자동 관리
  - 401 에러 시 리프레시 토큰으로 자동 재시도
  - FormData 및 JSON 요청 지원

- **`services/endpoints.ts`**: 도메인별 엔드포인트 정의
  - `authEndpoints`: 로그인, 회원가입, 이메일 인증
  - `boardEndpoints`: 게시판 CRUD, 좋아요, 페이지네이션
  - `commentEndpoints`: 댓글 CRUD

#### 페이지 구조
- `/` - 홈 (서비스 소개)
- `/post` - 게시판 목록 (카드 스타일)
- `/post/:id` - 게시판 상세 (댓글, 3D 뷰어)
- `/post/create` - 게시판 작성 (동영상 업로드)
- `/post/:id/edit` - 게시판 수정
- `/login` - 로그인
- `/register` - 회원가입
- `/profile` - 프로필 및 내 게시글

## 프로젝트 구조

```
memorylab/
├── memories_lab/                    # React 프론트엔드
│   ├── src/
│   │   ├── components/              # 재사용 컴포넌트
│   │   │   ├── Common/              # Header, Footer
│   │   │   ├── Post/                # BoardCard, PlyViewer, CommentList
│   │   │   └── Auth/                # PrivateRoute
│   │   ├── pages/                   # 라우트 페이지
│   │   │   ├── Home.tsx
│   │   │   ├── Post.tsx             # 게시판 목록
│   │   │   ├── BoardDetail.tsx      # 게시판 상세
│   │   │   ├── BoardCreate.tsx      # 게시판 작성
│   │   │   └── ...
│   │   ├── services/
│   │   │   ├── api.ts               # HTTP 클라이언트
│   │   │   └── endpoints.ts         # API 엔드포인트
│   │   ├── contexts/
│   │   │   └── AuthContext.tsx      # 인증 컨텍스트
│   │   ├── types/
│   │   │   └── api.ts               # API 타입 정의
│   │   └── utils/                   # 유틸리티 함수
│   └── vite.config.dev.ts           # Vite 설정 (API 프록시)
│
└── memorylab/                       # Spring Boot 백엔드
    ├── src/main/java/com/memorylab/
    │   ├── ai/                      # AI 서버 연동
    │   │   ├── AiServerClient.java
    │   │   ├── AiResultHandlerService.java
    │   │   └── AiCallbackController.java
    │   ├── config/                  # Spring 설정
    │   │   ├── SecurityConfig.java
    │   │   ├── WebClientConfig.java
    │   │   └── JwtProperties.java
    │   ├── controller/              # REST API 컨트롤러
    │   │   ├── AuthController.java
    │   │   ├── BoardController.java
    │   │   └── CommentController.java
    │   ├── domain/                  # JPA 엔티티
    │   │   ├── board/
    │   │   │   ├── Board.java       # 핵심 엔티티
    │   │   │   ├── BoardLike.java
    │   │   │   ├── BoardRepository.java
    │   │   │   └── *Status.java    # 상태 Enum들
    │   │   ├── comment/
    │   │   └── user/
    │   ├── repository/              # Spring Data JPA 리포지토리
    │   ├── service/                 # 비즈니스 로직
    │   │   ├── board/
    │   │   │   └── BoardService.java
    │   │   ├── auth/
    │   │   ├── mail/
    │   │   └── FileService.java
    │   ├── scheduler/               # 백그라운드 작업
    │   │   ├── ThumbnailScheduler.java
    │   │   ├── AiStatusScheduler.java
    │   │   └── CleanupScheduler.java
    │   └── dto/                     # DTO 클래스
    └── src/main/resources/
        └── application.yml          # 설정 파일
```

## 기술적 특징

### 1. 타입 안전 풀스택 아키텍처
- 프론트엔드: TypeScript로 전체 타입 안전성 확보
- 백엔드: Java 21의 강력한 타입 시스템
- API 계층: DTO로 계약 정의 및 검증

### 2. 반응형 비동기 처리
- Spring WebFlux의 WebClient로 AI 서버와 논블로킹 통신
- React의 useState/useEffect로 비동기 UI 상태 관리
- XMLHttpRequest로 파일 업로드 진행률 실시간 추적

### 3. 하이브리드 상태 동기화
- **주요 경로**: AI 서버 → Webhook → 즉시 상태 업데이트
- **대체 경로**: 스케줄러 → 60초 주기 폴링 → 누락 방지
- Circuit Breaker 패턴으로 AI 서버 장애 대응

### 4. 최적화된 데이터베이스 쿼리
- Spring Data JPA Projection으로 필요한 필드만 조회
- `@EntityGraph`로 N+1 문제 해결
- `@Formula`로 동적 집계 (likeCount, commentCount)

### 5. 보안 강화
- JWT Access Token (60분) + Refresh Token 조합
- 프론트엔드에서 자동 토큰 갱신 로직
- 웹훅 서명 검증으로 위조 요청 차단
- 역할 기반 접근 제어 (Spring Security)

### 6. 개발 경험 최적화
- Vite의 HMR로 빠른 개발 피드백
- 프록시 설정으로 CORS 문제 해결
- Lombok으로 보일러플레이트 감소
- Path Alias (`@/`) 로 깔끔한 import

## 개발 기간

2025년 8월 - 2025년 12월

---

<div align="center">

**3D Gaussian Splatting을 활용한 공간 보존 플랫폼**

</div>
