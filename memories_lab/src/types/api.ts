/**
 * TypeScript types matching backend DTOs
 */

// ==================== Enums ====================

export type Category = 'NOTICE' | 'SCENE' | 'OBJECT' | 'QNA';
export type Visibility = 'PUBLIC' | 'PRIVATE';
export type ConversionStatus =
  | 'NONE'
  | 'PENDING'
  | 'CONVERTING'
  | 'PROCESSING'
  | 'READY'
  | 'FAILED'
  | 'DISPATCHED'
  | 'DOWNLOADING'
  | 'RESULT_READY'
  | 'FAILED_PROCESS'
  | 'FAILED_DOWNLOAD';

// ==================== Auth DTOs ====================

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  nickname: string;
}

export interface SendVerificationCodeRequest {
  email: string;
}

export interface VerifyCodeRequest {
  email: string;
  code: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface RefreshResponse {
  accessToken: string;
}

export interface User {
  id: number | null;
  email: string;
  name: string;
  nickname: string;
  roles: string[];
  createdAt: string | null;
}

// ==================== Board DTOs ====================

export interface BoardCreateRequest {
  title: string;
  content: string;
  category: Category;
  visibility?: Visibility;
  tags?: string;
}

export interface BoardUpdateRequest {
  title: string;
  content: string;
  category: Category;
  visibility?: Visibility;
  tags?: string;
}

// Backend author structure
export interface Author {
  id: number;
  nickname: string;
}

// BoardSummary matches actual backend response from GET /api/board
export interface BoardSummary {
  id: number;
  title: string;
  author: Author;
  category: Category;
  visibility: Visibility;  // Required field from backend
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  isLikedByCurrentUser: boolean;
  thumbnailPath: string | null;
  thumbnailStatus: string;  // "NONE" | "PENDING" | "READY" | "FAILED"
  hasVideo: boolean;  // 동영상 파일 업로드 여부
  status: ConversionStatus;  // Board processing status
  progress?: number | null;
  errorMessage?: string | null;
  tags?: string | null;
}

// BoardDetail matches actual backend response from GET /api/board/:id
export interface BoardDetail {
  id: number;
  title: string;
  content: string;
  // Backend returns flat author fields, not nested object
  authorId: number;
  authorNickname: string;
  category: Category;
  visibility: Visibility;
  viewCount: number;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  modifiedAt: string;  // Backend uses 'modifiedAt' not 'updatedAt'
  isLikedByCurrentUser: boolean;
  thumbnailPath: string | null;
  thumbnailStatus: string;
  convertedVideoPath: string | null;
  plyPath: string | null;
  aiTaskId: string | null;
  gpuErrorMessage: string | null;
  externalErrorCode: string | null;
  externalErrorDetail: string | null;
  transcodeStatus: string;
  hasVideo: boolean;  // 동영상 파일 업로드 여부
  status: ConversionStatus;
  progress?: number | null;
  errorMessage?: string | null;
  tags?: string | null;
}

// Backend pagination response structure
export interface BoardPageResponse {
  content: BoardSummary[];
  totalPages: number;
  totalElements: number;
  currentPage: number;  // Backend uses "currentPage" not "number"
  pageSize: number;     // Backend uses "pageSize" not "size"
  isFirst: boolean;     // Backend uses "isFirst" not "first"
  isLast: boolean;      // Backend uses "isLast" not "last"
}

// ==================== Comment DTOs ====================

export interface CommentCreateRequest {
  content: string;
  parentId?: number | null;
}

export interface CommentUpdateRequest {
  content: string;
}

export interface Comment {
  id: number;
  content: string;
  boardId: number;
  authorId: number;
  authorNickname: string;
  parentId: number | null;
  createdAt: string;
  modifiedAt: string;
}

export interface CommentPageResponse {
  content: Comment[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// ==================== Like DTOs ====================

export interface LikeResponse {
  isLiked: boolean;
  likeCount: number;
}

// ==================== Error Response ====================

export interface ApiError {
  message: string;
  status?: number;
  timestamp?: string;
  path?: string;
}

// ==================== UI Helper Types ====================

export const CATEGORY_LABELS: Record<Category, string> = {
  NOTICE: '공지사항',
  SCENE: '장면',
  OBJECT: '물체',
  QNA: '문의하기',
};

export const VISIBILITY_LABELS: Record<Visibility, string> = {
  PUBLIC: '전체공개',
  PRIVATE: '비공개',
};

export const CATEGORY_MAP: Record<string, Category> = {
  '공지사항': 'NOTICE',
  '장면': 'SCENE',
  '물체': 'OBJECT',
  '문의하기': 'QNA',
};

export const VISIBILITY_MAP: Record<string, Visibility> = {
  '전체공개': 'PUBLIC',
  '비공개': 'PRIVATE',
};

// Helper to convert Korean labels to backend enums
export function categoryToEnum(label: string): Category {
  return CATEGORY_MAP[label] || 'SCENE';
}

export function visibilityToEnum(label: string): Visibility {
  return VISIBILITY_MAP[label] || 'PUBLIC';
}

// Helper to convert backend enums to Korean labels
export function categoryToLabel(category: Category): string {
  return CATEGORY_LABELS[category] || category;
}

export function visibilityToLabel(visibility: Visibility): string {
  return VISIBILITY_LABELS[visibility] || visibility;
}
