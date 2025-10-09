# Integration Testing Guide

## Current Status: ✅ Frontend Dev Server Running

**Dev Server**: `http://localhost:5173`
**Backend API**: Should proxy through Vite to backend (configured for `https://mlab.snowytiger.me`)

---

## What's Been Built and Integrated

### ✅ Phase 1: Infrastructure (COMPLETE)

#### API Layer
- **`src/services/api.ts`**: Fetch-based HTTP client with:
  - Automatic JWT token handling (localStorage)
  - Token refresh on 401 (calls `/api/auth/refresh`)
  - Automatic redirect to `/login` on auth failure
  - Support for FormData (multipart uploads)

#### Type System
- **`src/types/api.ts`**: Full TypeScript definitions for:
  - Auth: `LoginRequest`, `RegisterRequest`, `User`, tokens
  - Boards: `BoardSummary`, `BoardDetail`, `BoardPageResponse`
  - Comments: `Comment`, `CommentPageResponse`
  - Enums: `Category`, `Visibility`, `ConversionStatus`
  - Helper functions: `categoryToEnum()`, `visibilityToLabel()`, etc.

#### API Endpoints
- **`src/services/endpoints.ts`**: Type-safe API methods:
  - `authEndpoints`: login, register, email verification flow, me
  - `boardEndpoints`: list, detail, create, update, delete, toggleLike
  - `commentEndpoints`: list, create, update, delete
  - Upload progress tracking for file uploads

#### Configuration
- **`vite.config.dev.ts`**: Proxy `/api` → `https://mlab.snowytiger.me`
  - Alternative commented: `http://54.180.3.34`
  - CORS-ready, credentials forwarded

### ✅ Phase 2: Backend Configuration (COMPLETE)

#### SPA Fallback
- **`WebMvcConfig.java`**: Added view controllers for deep linking
  - All non-API routes forward to `index.html`
  - Enables React Router refresh without 404

### ✅ Phase 3: Authentication (COMPLETE)

#### Context & Hooks
- **`src/contexts/AuthContext.tsx`**: Global auth state
  - Login/logout/register methods
  - Email verification helpers
  - Auto-fetch user on mount if token exists
- **`src/hooks/useAuth.ts`**: Convenient context access

#### Pages
- **`src/pages/Login.tsx`**:
  - Email/password form
  - Calls `POST /api/auth/login`
  - Saves tokens, navigates to home
  - Error handling

- **`src/pages/Register.tsx`**:
  - Multi-step wizard (email → verify → details)
  - Email/nickname availability checks
  - Email verification code flow
  - Password confirmation
  - Full error handling

#### Routing
- **`src/App.tsx`**:
  - Wrapped in `AuthProvider`
  - Routes: `/`, `/login`, `/register`, `/post`, `/post/:id`, `/post/create`
  - Protected route wrapper (requires auth for `/post/create`)

### ✅ Phase 4: Board List (COMPLETE)

#### Components
- **`src/components/Post/Board.tsx`**:
  - Updated to accept `BoardSummary` type
  - Displays thumbnail or status badge (PROCESSING/FAILED)
  - Shows progress percentage if converting
  - Navigate to detail on click
  - Category label, formatted date

- **`src/components/Post/BoardList.tsx`**:
  - Fetches from `GET /api/board?page=0&size=12`
  - Pagination controls (prev/next, page numbers)
  - Loading spinner
  - Error state with retry button
  - Empty state

#### Styling
- Added CSS for: loading spinner, pagination buttons, error/empty states

---

## Testing Steps

### 1. Backend Verification (Do This First!)

#### Check if Backend is Running
```bash
# Test if backend is accessible
curl https://mlab.snowytiger.me/api/auth/check-email?email=test@example.com

# OR test fallback IP
curl http://54.180.3.34/api/auth/check-email?email=test@example.com
```

**Expected**:
- 200 OK (email available) or 409 Conflict (email taken)
- **NOT** connection error or CORS error

#### Start Backend (if needed)
```bash
cd memorylab
./gradlew bootRun
```

### 2. Frontend Testing (Dev Server Already Running ✅)

#### Open Browser
Navigate to: `http://localhost:5173`

#### Test Home Page
- Should load without errors
- Navigation should be visible

#### Test Authentication Flow

**A. Registration:**
1. Click "회원가입" or navigate to `/register`
2. Enter email → should check availability
3. Click "인증 코드 받기" → email should be sent (check backend logs)
4. Enter verification code from email
5. Fill in name, nickname, password
6. Check nickname availability
7. Complete registration
8. Should redirect to `/login`

**Expected API Calls**:
- `GET /api/auth/check-email?email=...`
- `POST /api/auth/send-verification-code`
- `POST /api/auth/verify-code`
- `GET /api/auth/check-nickname?nickname=...`
- `POST /api/auth/register`

**B. Login:**
1. Navigate to `/login`
2. Enter registered credentials
3. Click "로그인"
4. Should save tokens to localStorage
5. Should redirect to home
6. Should see user info in AuthContext

**Expected API Calls**:
- `POST /api/auth/login` → returns `{ accessToken, refreshToken }`
- `GET /api/auth/me` → returns user profile

#### Test Board List

**Without Authentication:**
1. Navigate to `/post`
2. Should see board list (public boards only)
3. Pagination should work if >12 boards

**Expected API Calls**:
- `GET /api/board?page=0&size=12&sort=createdAt,desc`

**Expected Response Structure**:
```json
{
  "content": [
    {
      "id": 1,
      "title": "...",
      "category": "FREE",
      "visibility": "PUBLIC",
      "thumbnailUrl": "...",
      "conversionStatus": "READY",
      "progress": null,
      "errorMessage": null,
      "tags": null,
      "viewCount": 10,
      "createdAt": "2025-10-08T...",
      "authorNickname": "..."
    }
  ],
  "totalPages": 1,
  "totalElements": 5,
  "size": 12,
  "number": 0
}
```

#### Test Protected Routes

**Without Login:**
1. Try to navigate to `/post/create`
2. Should redirect to `/login`

**After Login:**
1. Navigate to `/post/create`
2. Should show create form

---

## Known Limitations (Not Yet Integrated)

### 🔄 Board Detail Page
- Currently shows placeholder UI
- **Needs**: API integration to fetch `GET /api/board/:id`
- **Needs**: Comment list/create UI
- **Needs**: Like button integration
- **Needs**: Progress polling for converting boards

### 🔄 Board Create Page
- Has UI but not connected to API
- **Needs**: FormData upload with progress tracking
- **Needs**: Category/visibility mapping (Korean → English enums)
- **Needs**: Error handling for 500MB limit
- **Needs**: Success redirect to detail page

### 🔄 Comments
- Backend ready, frontend not integrated
- **Endpoints**:
  - `POST /api/board/:boardId/comments`
  - `GET /api/board/:boardId/comments`
  - `PUT /api/comments/:commentId`
  - `DELETE /api/comments/:commentId`

---

## Browser DevTools Checklist

### Network Tab
- Check that `/api/*` requests go through Vite proxy
- Verify `Authorization: Bearer <token>` header on authenticated requests
- Check for CORS errors (there should be NONE)
- Verify 401 triggers token refresh attempt

### Console Tab
- Should have no React errors
- Should have no TypeScript errors
- May have proxy logs (normal)

### Application Tab → Local Storage
- After login, should see:
  - `accessToken`: JWT string
  - `refreshToken`: JWT string

---

## Backend Compatibility Checklist

### DTO Field Names (CRITICAL!)

Our frontend expects these field names from backend:

**Board Summary** (`GET /api/board`):
```typescript
{
  id: number;
  title: string;
  category: "NOTICE" | "FREE" | "QNA";
  visibility: "PUBLIC" | "PRIVATE";
  thumbnailUrl: string | null;
  conversionStatus: string;
  progress: number | null;
  errorMessage: string | null;
  tags: string | null;
  viewCount: number;
  createdAt: string;  // ISO 8601
  authorNickname: string;
}
```

**Auth Login Response** (`POST /api/auth/login`):
```typescript
{
  accessToken: string;
  refreshToken: string;
}
```

**User Profile** (`GET /api/auth/me`):
```typescript
{
  id: number | null;
  email: string;
  name: string;
  nickname: string;
  roles: string[];  // e.g., ["ROLE_USER"]
  createdAt: string | null;
}
```

### Error Response Format

Frontend expects errors as:
```typescript
{
  message: string;
  status?: number;
  timestamp?: string;
  path?: string;
}
```

---

## Troubleshooting

### "Failed to fetch" errors
- Backend not running
- CORS not configured
- Wrong proxy target in `vite.config.dev.ts`

### 401 Unauthorized loops
- Token refresh endpoint broken
- Invalid refresh token
- Check localStorage tokens

### CORS errors
- Backend SecurityConfig needs dev origin
- Should allow: `http://localhost:5173`

### Pagination not working
- Backend returning wrong `Page<T>` structure
- Check `totalPages`, `number`, `content` fields

### Upload fails
- Check backend `max-file-size: 500MB` in `application.yml`
- Check Nginx `client_max_body_size`

---

## Next Steps

1. ✅ **Verify backend is accessible** (curl tests above)
2. ✅ **Test authentication flow** (register → login)
3. ✅ **Test board list fetching**
4. 🔄 **Integrate board detail page** (fetch, poll, like, comments)
5. 🔄 **Integrate board create** (upload with progress)
6. 🔄 **Test full flow end-to-end**
7. 🔄 **Production build test**

---

## Dev Server Info

**Status**: ✅ Running
**URL**: http://localhost:5173
**Command**: `npm run dev` (in `memories_lab/`)
**Process ID**: Check with `/bashes` command

To stop server: Use `KillShell` tool with the process ID.
