# Application Routing Guide

**Last Updated**: 2025-10-08
**Status**: ✅ Complete SPA Routing

---

## Route Map

### Public Routes

| Route | Component | Description | Features |
|-------|-----------|-------------|----------|
| `/` | `Home` | Landing page | Hero, CTA sections, navigation to /post |
| `/login` | `Login` | User login | JWT authentication, redirect after login |
| `/register` | `Register` | User registration | 3-step wizard, email verification, nickname check |
| `/post` | `Post` | Board list | Pagination, search, category filter, skeleton loaders |
| `/post/:id` | `BoardDetail` | Board detail | Comments, likes, status polling, breadcrumbs |

### Protected Routes

| Route | Component | Description | Auth Required | Redirect if Unauthorized |
|-------|-----------|-------------|---------------|--------------------------|
| `/post/create` | `BoardCreate` | Create new board | ✅ | → `/login` |

### Error Routes

| Route | Component | Description |
|-------|-----------|-------------|
| `*` (404) | `NotFound` | Page not found |
| `/error` (optional) | `ServerError` | Server error (500) |

---

## Route Details

### Home `/`

**Purpose**: Landing page with hero section and CTA

**Navigation Options**:
- Logo → `/` (stays on home)
- "시작하기" button → `/post`
- Login/Register links in navigation

**No Authentication Required**

---

### Login `/login`

**Purpose**: User authentication

**Features**:
- Email + password form
- JWT token storage (accessToken + refreshToken)
- Auto-redirect to previous page after login
- Link to `/register` for new users

**API Calls**:
- `POST /api/auth/login`
- `GET /api/auth/me` (after login)

**Redirect Logic**:
- If already logged in → `/`
- After successful login → `/` or previous protected route

---

### Register `/register`

**Purpose**: User registration with email verification

**Features**:
- 3-step wizard:
  1. Email input + availability check
  2. Email verification code
  3. Complete profile (name, nickname, password)
- Real-time validation
- Nickname uniqueness check

**API Calls**:
- `GET /api/auth/check-email?email={email}`
- `POST /api/auth/send-verification-code`
- `POST /api/auth/verify-code`
- `GET /api/auth/check-nickname?nickname={nickname}`
- `POST /api/auth/register`

**Redirect Logic**:
- After successful registration → `/login`

---

### Board List `/post`

**Purpose**: Display all boards with pagination

**Features**:
- Pagination (12 items per page)
- Skeleton loaders during fetch
- Empty state with "Create Board" CTA
- Category badges (공지/자유/QNA)
- Like count, comment count, view count
- Click card → `/post/:id`

**API Calls**:
- `GET /api/board?page={p}&size={s}&sort={sort}`

**Breadcrumbs**: None (top-level)

**No Authentication Required** (but shows login prompt if trying to create)

---

### Board Detail `/post/:id`

**Purpose**: Display single board with comments

**Features**:
- Board content (title, author, category, stats)
- Like/Unlike button (requires auth)
- Status polling for PROCESSING boards (every 5 seconds)
- Comments section (CRUD)
- Nested replies (parentId support)
- Breadcrumbs: 홈 → 게시글 → 게시글 #{id}

**API Calls**:
- `GET /api/board/:id`
- `POST /api/board/:id/like` (protected)
- `GET /api/board/:boardId/comments`
- `POST /api/board/:boardId/comments` (protected)
- `PUT /api/comments/:commentId` (protected, owner only)
- `DELETE /api/comments/:commentId` (protected, owner only)

**Redirect Logic**:
- If board not found → 404 page
- If like/comment without auth → `/login`

**Breadcrumbs**: Yes

---

### Board Create `/post/create` (Protected)

**Purpose**: Create new board with optional MP4 upload

**Features**:
- Title, content, category, visibility form
- MP4 file upload (drag & drop, ≤ 500MB)
- Live upload progress (0-100%)
- Category mapping (Korean → Enum)
- Character count for content (max 5000)
- Breadcrumbs: 홈 → 게시글 → 새 글 작성

**API Calls**:
- `POST /api/board` (multipart/form-data)

**Redirect Logic**:
- If not authenticated → `/login`
- After successful creation → `/post/:id` (newly created board)

**Breadcrumbs**: Yes

**Authentication**: Required

---

### Not Found `*` (404)

**Purpose**: Catch-all for unknown routes

**Features**:
- 404 error code display
- Friendly error message
- "이전 페이지" button (navigate(-1))
- "홈으로 돌아가기" button → `/`

**Navigation**: Always accessible

---

## Navigation Flow

### User Journey: First Visit

```
1. Land on / (Home)
2. Click "시작하기" → /post (Board List)
3. Click a board card → /post/16 (Board Detail)
4. Try to like → Redirect to /login
5. Login successful → Back to /post/16
6. Add comment, like post
7. Click "게시글 작성하기" → /post/create
8. Upload video, submit → /post/17 (new board detail)
```

### User Journey: Logged-In User

```
1. Land on / (Home)
2. Click "시작하기" → /post
3. Click "게시글 작성하기" → /post/create
4. Create board → /post/17
5. View own board, edit comments
6. Navigate back to list → /post
7. Logout → Stay on current page
```

---

## Deep Linking & SPA Fallback

### Supported Deep Links

All routes support deep linking (manual URL entry or refresh):

- ✅ `/post` - Works
- ✅ `/post/16` - Works
- ✅ `/post/create` - Works (redirects to login if not auth)
- ✅ `/login?redirect=/post/create` - Works (after login, goes to redirect)
- ✅ `/random-path` - Shows 404

**Backend SPA Fallback** (Spring WebMvcConfig):
```java
@Override
public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/{spring:[^\\.]*}")
            .setViewName("forward:/index.html");
    registry.addViewController("/**/{spring:[^\\.]*}")
            .setViewName("forward:/index.html");
}
```

---

## Scroll Behavior

### Scroll to Top

**When**: Route change (pathname change)
**Implementation**: `ScrollToTop` component
**Behavior**: Instant scroll to top (no smooth scroll to avoid janky UX)

### Scroll Restoration

**When**: Browser back/forward navigation
**Implementation**: Browser's native `history.scrollRestoration = 'auto'`
**Behavior**: Restores scroll position when using back button

---

## Route Guards & Protection

### ProtectedRoute Component

**Purpose**: Wraps routes that require authentication

**Logic**:
```typescript
function ProtectedRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) return <div>Loading...</div>;
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}
```

**Usage**:
```tsx
<Route
  path="/post/create"
  element={
    <ProtectedRoute>
      <BoardCreate />
    </ProtectedRoute>
  }
/>
```

**Protected Routes**:
- `/post/create`
- (Future) `/profile`, `/settings`, `/admin`

---

## URL Parameters

### Board Detail `/post/:id`

**Parameter**: `id` (number)
**Example**: `/post/16`
**Access**: `useParams<{ id: string }>()` → `const { id } = useParams();`

**Validation**:
- If `id` is not a valid number → 404
- If board doesn't exist → 404 from API → Error component

---

## Query Parameters (Future)

### Board List `/post?page=2&category=FREE&sort=createdAt,desc`

**Current**: Only `page` is used internally
**Future Enhancements**:
- `?category=FREE` - Filter by category
- `?sort=popular` - Sort by likes
- `?search=keyword` - Search boards

---

## Navigation Methods

### Programmatic Navigation

```typescript
import { useNavigate } from 'react-router-dom';

const navigate = useNavigate();

// Navigate to path
navigate('/post');

// Navigate with replace (no history entry)
navigate('/login', { replace: true });

// Go back
navigate(-1);

// Go forward
navigate(1);
```

### Link Navigation

```tsx
import { Link } from 'react-router-dom';

<Link to="/post">게시글</Link>
<Link to={`/post/${board.id}`}>View Board</Link>
```

### Button Navigation

```tsx
<button type="button" onClick={() => navigate('/post')}>
  게시글 보기
</button>
```

---

## Page Transitions

### Framer Motion Wrapper

**Current**: Fade-in animation on route change
**Duration**: 0.3s
**Implementation**:
```tsx
<motion.div
  initial={{ opacity: 0 }}
  animate={{ opacity: 1 }}
  transition={{ duration: 0.3 }}
>
  <Routes>...</Routes>
</motion.div>
```

**Future Enhancements**:
- Slide transitions for modal-like routes
- Shared element transitions for image galleries

---

## Breadcrumbs

### Routes with Breadcrumbs

- `/post/:id` → 홈 / 게시글 / 게시글 #{id}
- `/post/create` → 홈 / 게시글 / 새 글 작성

### Implementation

```tsx
<Breadcrumbs
  items={[
    { label: '홈', path: '/' },
    { label: '게시글', path: '/post' },
    { label: '새 글 작성' },
  ]}
/>
```

**Styling**: Consistent across pages, clickable links for parent routes

---

## Error Handling

### 404 Not Found

**Triggered By**:
- Invalid route (e.g., `/asdfasdf`)
- Board not found (handled by API)

**Display**: Custom NotFound component with navigation options

### 500 Server Error

**Triggered By**:
- API errors (can be caught and displayed inline)
- Future: Global error boundary

**Display**: Custom ServerError component with retry option

---

## Testing Routes

### Manual Testing Checklist

- [x] `/` loads correctly
- [x] Navigate from `/` to `/post`
- [x] Navigate from `/post` to `/post/16`
- [x] Back button returns to `/post`
- [x] Refresh on `/post/16` works
- [x] `/post/create` redirects to `/login` if not authenticated
- [x] After login, redirect back to `/post/create`
- [x] After creating board, navigate to new board detail
- [x] `/random-path` shows 404
- [x] 404 page "홈으로" button works
- [x] All links in navigation work
- [x] Breadcrumbs are clickable and navigate correctly

---

## Future Route Additions

### User Profile

- `/profile/:id` or `/users/:id` - View user profile
- `/profile/edit` or `/settings` - Edit own profile (protected)

### Admin Routes

- `/admin` - Admin dashboard (protected, role-based)
- `/admin/users` - Manage users
- `/admin/boards` - Manage boards

### Additional Features

- `/tags/:tag` - Filter by tag
- `/search?q=keyword` - Search results
- `/notifications` - User notifications (protected)

---

## Route Configuration

### Vite Dev Proxy

**File**: `vite.config.dev.ts`

```typescript
server: {
  proxy: {
    '/api': {
      target: 'https://mlab.snowytiger.me',
      changeOrigin: true,
      secure: true,
    },
  },
}
```

**All API calls** use relative `/api` paths, proxied to backend in dev, routed via Nginx in production.

---

## Summary

✅ **All routes are SPA routes** (no server-rendered pages)
✅ **Deep linking works** (SPA fallback configured)
✅ **Scroll management** (scroll to top on route change)
✅ **Protected routes** (redirect to login if not authenticated)
✅ **Breadcrumbs** on detail and create pages
✅ **404 handling** (catch-all route)
✅ **Smooth transitions** (Framer Motion)

**No dead links, all navigation is functional!**
