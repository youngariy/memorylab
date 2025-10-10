# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MemoryLab (추억현상소) is a web archiving platform for recording and sharing disappearing spaces through digital 3D reconstruction. The project consists of a React frontend and Spring Boot backend in a monorepo structure.

## Project Structure

- **`memories_lab/`**: React + TypeScript frontend (Vite)
- **`memorylab/`**: Spring Boot backend (Java 21, Gradle)

## Frontend Development (`memories_lab/`)

### Commands

```bash
# Install dependencies
pnpm install

# Development server (http://localhost:5173)
pnpm dev

# Production build
pnpm build

# Preview production build
pnpm preview

# Lint code
pnpm lint
```

### Technology Stack

- React 18.2 + TypeScript
- Vite 5.0 (build tool)
- React Router 6.20 (routing)
- Framer Motion 10.16 (animations)
- Radix UI (UI components: Dialog, NavigationMenu, ScrollArea, Select)
- Lucide React (icons)

### Routing Structure

- `/` - Home page with service introduction
- `/post` - Board list page with card-style previews
- `/post/:id` - Board detail page with comments and 3D model status
- `/post/create` - Board creation page with MP4 file upload (drag & drop, max 500MB)

### Key Features

- **Responsive Design**: Mobile (≤768px), Tablet (769-1023px), Desktop (≥1024px)
- **Video Upload**: MP4 file validation and drag-and-drop support (max 500MB)
- **Upload Progress Tracking**: XMLHttpRequest-based progress monitoring during file upload
- **3D Visualization**: Uses `@mkkellogg/gaussian-splats-3d` to render PLY files when status is `READY`

### Path Alias

The `@` alias points to `./src` (configured in `vite.config.dev.ts`)

### API Proxy Configuration

The frontend proxies `/api` requests to the backend:
- **Development**: `http://localhost:8080` (run backend with `./gradlew bootRun`)
- **Production**: `https://mlab.snowytiger.me`
- Configuration in `vite.config.dev.ts:19-30`

### Development Workflow

1. **Start Backend**: `cd memorylab && ./gradlew bootRun` (or `.bat` on Windows)
2. **Start Frontend**: `cd memories_lab && pnpm dev`
3. **Access**: Frontend runs on `http://localhost:5173`, API calls proxy to backend at `:8080`

### Frontend Service Layer

- **`services/api.ts`**: Base HTTP client with JWT token management and automatic refresh
- **`services/endpoints.ts`**: Type-safe API endpoint definitions organized by domain:
  - `authEndpoints`: Authentication (login, register, email verification)
  - `boardEndpoints`: Board CRUD, likes, with pagination support
  - `commentEndpoints`: Comment CRUD for boards
- All API types defined in `types/api.ts` for end-to-end type safety

## Backend Development (`memorylab/`)

### Commands

```bash
# Run application
./gradlew bootRun                # Unix/macOS
./gradlew.bat bootRun            # Windows

# Build project
./gradlew build                  # Unix/macOS
./gradlew.bat build              # Windows

# Run tests
./gradlew test                   # Unix/macOS
./gradlew.bat test               # Windows

# Clean build
./gradlew clean build            # Unix/macOS
./gradlew.bat clean build        # Windows
```

### Technology Stack

- **Java 21** with Spring Boot 3.3.1
- **Spring Security** + JWT authentication (io.jsonwebtoken 0.12.5)
- **Spring Data JPA** with MySQL (supports H2 for development)
- **Spring WebFlux** (WebClient for AI server integration)
- **Resilience4j** (circuit breaker)
- **ShedLock** (distributed scheduling)
- **Lombok** (reduce boilerplate)

### Architecture

#### Core Domain: Board Entity

The `Board` entity (`com.memorylab.domain.board.Board`) is the central domain model representing user-uploaded content. It tracks the entire lifecycle from video upload through AI processing to PLY file generation and display:

**File Paths**:
- `originalVideoPath`: User-uploaded MP4 file
- `convertedVideoPath`: Transcoded video for web playback
- `thumbnailPath`: Generated thumbnail image
- `plyPath`: 3D Gaussian Splatting PLY file (final output)

**Status Fields** (multiple independent status trackers):
- `TranscodeStatus`: Video conversion state (NONE, PENDING, CONVERTING, READY, FAILED)
- `ThumbnailStatus`: Thumbnail generation state (NONE, PENDING, READY, FAILED)
- `BoardStatus`: Internal application state (DISPATCHED, PROCESSING, RESULT_READY, DOWNLOADING, READY, FAILED_PROCESS, FAILED_DOWNLOAD)
- `ExternalStatus`: AI server task state (QUEUED, PROCESSING, COMPLETED, FAILED)

**AI Integration**:
- `aiTaskId`: Unique task identifier from external AI server
- `externalResultUrl`: URL to download completed PLY file
- `externalErrorCode`, `externalErrorDetail`: Error tracking from AI server

#### AI Processing Workflow

1. **Upload**: User uploads MP4 → `BoardService.createBoard()` saves file locally
2. **AI Dispatch**: Backend sends file URL to AI server → receives `aiTaskId` → sets `ExternalStatus.QUEUED`
3. **Processing**: AI server performs 3D Gaussian Splatting reconstruction
4. **Status Updates** (hybrid approach):
   - **Primary**: Webhook callbacks via `AiResultHandlerService` (signature-validated)
   - **Fallback**: `AiStatusScheduler` polls every 60s for tasks in `QUEUED`/`PROCESSING` state
5. **Download**: Backend downloads PLY file from `externalResultUrl`
6. **Ready**: Frontend displays "3D 보기" button → renders PLY using React Three Fiber

#### Key Services

**`BoardService`** (`com.memorylab.service.board.BoardService`):
- Core CRUD operations for boards
- `mapToBoardStatus()`: Derives user-facing status from multiple status fields
- Handles file upload and integrates with `AiServerClient`

**`AiServerClient`** (`com.memorylab.ai.AiServerClient`):
- WebClient-based communication with AI server
- `requestUpload()`: Sends video file URL to AI server for processing
- `deleteAiResource()`: Cleans up AI server resources when board is deleted

**`AiResultHandlerService`** (`com.memorylab.ai.AiResultHandlerService`):
- Processes webhook callbacks from AI server
- `queuePlyDownload()`: Handles successful completion
- `processFailedTask()`: Handles processing failures

**Schedulers**:
- `ThumbnailScheduler`: Generates thumbnails from videos (triggered when `ThumbnailStatus.PENDING`)
- `CleanupScheduler`: Removes old/abandoned resources
- `AiStatusScheduler`: Polls AI server every 60 seconds for tasks with status `QUEUED` or `PROCESSING`
- Note: `ConversionPollingScheduler` is deprecated (replaced by webhook system)

#### Security

- JWT-based authentication with refresh tokens (`RefreshToken` entity)
- Email verification system (`VerificationCode` entity, `EmailService`)
- Spring Security configuration with role-based access (ROLE_ADMIN, ROLE_USER)
- Signature validation for webhook callbacks (`SignatureValidator`)

#### Repository Layer

- JPA repositories with Spring Data projections for optimized queries
- `BoardRepository`: Custom queries with `@EntityGraph` for efficient loading
- Projection interfaces: `BoardDetailProj`, `BoardSummaryProj`, `AuthorProj`

### Configuration

- **Profiles**: `application.yml` (default), `application-prod.yml` (production)
- **Key Properties**:
  - `file.storage.*`: File storage paths for videos, thumbnails, PLY files
  - `gpu-server.base-url`: External AI server endpoint (changed from `ai.server.base-url`)
  - `gpu-server.webhook-secret`: Webhook signature validation secret
  - `app.upload.root-dir`: Root directory for file uploads
  - JWT configuration in `JwtProperties`
- **Environment Variables** (required):
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET`, `GPU_WEBHOOK_SECRET`
  - `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`

### External Dependencies

- **@mkkellogg/gaussian-splats-3d**: 3D Gaussian Splatting rendering library (v0.4.7)
- **@react-three/fiber** & **@react-three/drei**: React bindings for Three.js
- Frontend uses these libraries to render PLY files when board status is `READY`
- Implementation in `memories_lab/src/components/Post/PlyViewer.tsx`

## Development Notes

- The project is actively developed (2025.08 - 2025.12 timeline)
- Use `pnpm` for frontend dependency management (preferred over npm)
- Backend requires Java 21 toolchain
- AI processing uses hybrid approach: webhook callbacks (primary) + polling fallback (60s interval)
- Windows development: Use `./gradlew.bat` instead of `./gradlew` for all Gradle commands
