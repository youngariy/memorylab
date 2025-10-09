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
- **Video Upload**: MP4 file validation and drag-and-drop support
- **3D Visualization**: Uses Spark.js (MIT license) to render PLY files with Three.js when status is `READY`

### Path Alias

The `@` alias points to `./src` (configured in `vite.config.dev.ts`)

## Backend Development (`memorylab/`)

### Commands

```bash
# Run application
./gradlew bootRun

# Build project
./gradlew build

# Run tests
./gradlew test

# Clean build
./gradlew clean build
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
4. **Webhook Callback**: AI server notifies completion → `AiResultHandlerService` updates status
5. **Download**: Backend downloads PLY file from `externalResultUrl`
6. **Ready**: Frontend displays "3D 보기" button → renders PLY with Spark.js + Three.js

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
- `ThumbnailScheduler`: Generates thumbnails from videos
- `CleanupScheduler`: Removes old/abandoned resources
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
  - `ai.server.base-url`: External AI server endpoint
  - JWT configuration in `JwtProperties`

### External Dependencies

- **Spark.js**: 3D Gaussian splat rendering library (MIT license, Three.js compatible)
- GitHub: https://github.com/sparkjsdev/spark

## Development Notes

- The project is actively developed (2025.08 - 2025.12 timeline)
- Use `pnpm` for frontend dependency management (preferred over npm)
- Backend requires Java 21 toolchain
- AI processing is asynchronous via webhook callbacks (not polling)
