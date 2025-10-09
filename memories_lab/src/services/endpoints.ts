/**
 * Type-safe API endpoint service
 * Centralized API calls for MemoryLab backend
 */

import api from './api';
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  SendVerificationCodeRequest,
  VerifyCodeRequest,
  RefreshRequest,
  RefreshResponse,
  User,
  BoardCreateRequest,
  BoardUpdateRequest,
  BoardDetail,
  BoardPageResponse,
  CommentCreateRequest,
  CommentUpdateRequest,
  Comment,
  CommentPageResponse,
  LikeResponse,
} from '../types/api';

// ==================== Auth Endpoints ====================

export const authEndpoints = {
  /**
   * Check if email is available
   */
  checkEmail: (email: string) =>
    api.get<void>(`/auth/check-email?email=${encodeURIComponent(email)}`, { skipAuth: true }),

  /**
   * Check if nickname is available
   */
  checkNickname: (nickname: string) =>
    api.get<void>(`/auth/check-nickname?nickname=${encodeURIComponent(nickname)}`, { skipAuth: true }),

  /**
   * Send email verification code
   */
  sendVerificationCode: (data: SendVerificationCodeRequest) =>
    api.post<void>('/auth/send-verification-code', data, { skipAuth: true }),

  /**
   * Verify email code
   */
  verifyCode: (data: VerifyCodeRequest) =>
    api.post<void>('/auth/verify-code', data, { skipAuth: true }),

  /**
   * Register new user
   */
  register: (data: RegisterRequest) =>
    api.post<void>('/auth/register', data, { skipAuth: true }),

  /**
   * Login
   */
  login: (data: LoginRequest) =>
    api.post<LoginResponse>('/auth/login', data, { skipAuth: true }),

  /**
   * Refresh access token
   */
  refresh: (data: RefreshRequest) =>
    api.post<RefreshResponse>('/auth/refresh', data, { skipAuth: true }),

  /**
   * Get current user profile
   */
  me: () =>
    api.get<User>('/auth/me'),
};

// ==================== Board Endpoints ====================

export const boardEndpoints = {
  /**
   * Get board list with pagination
   */
  list: (params: { page?: number; size?: number; sort?: string } = {}) => {
    const { page = 0, size = 12, sort = 'createdAt,desc' } = params;
    const queryParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort,
    });
    return api.get<BoardPageResponse>(`/board?${queryParams}`);
  },

  /**
   * Get current user's board list
   */
  myBoards: (params: { page?: number; size?: number; sort?: string } = {}) => {
    const { page = 0, size = 12, sort = 'createdAt,desc' } = params;
    const queryParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
      sort,
    });
    return api.get<BoardPageResponse>(`/board/me?${queryParams}`);
  },

  /**
   * Get board detail by ID
   */
  detail: (id: number) =>
    api.get<BoardDetail>(`/board/${id}`),

  /**
   * Create new board with optional video file
   */
  create: (data: BoardCreateRequest, videoFile?: File, onProgress?: (progress: number) => void) => {
    const formData = new FormData();

    // Append JSON data as 'req' part
    const jsonBlob = new Blob([JSON.stringify(data)], { type: 'application/json' });
    formData.append('req', jsonBlob);

    // Append video file if provided
    if (videoFile) {
      formData.append('videoFile', videoFile);
    }

    // Use XMLHttpRequest for progress tracking
    if (onProgress && videoFile) {
      return new Promise<BoardDetail>((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        const token = localStorage.getItem('accessToken');

        xhr.upload.addEventListener('progress', (e) => {
          if (e.lengthComputable) {
            const percentComplete = Math.round((e.loaded / e.total) * 100);
            onProgress(percentComplete);
          }
        });

        xhr.addEventListener('load', () => {
          if (xhr.status >= 200 && xhr.status < 300) {
            try {
              const response = JSON.parse(xhr.responseText);
              resolve(response);
            } catch (e) {
              reject(new Error('Failed to parse response'));
            }
          } else {
            reject(new Error(`Upload failed with status ${xhr.status}`));
          }
        });

        xhr.addEventListener('error', () => {
          reject(new Error('Upload failed'));
        });

        xhr.addEventListener('abort', () => {
          reject(new Error('Upload cancelled'));
        });

        xhr.open('POST', '/api/board');
        if (token) {
          xhr.setRequestHeader('Authorization', `Bearer ${token}`);
        }
        xhr.send(formData);
      });
    }

    // Fallback to regular fetch if no progress tracking needed
    return api.post<BoardDetail>('/board', formData);
  },

  /**
   * Update board
   */
  update: (id: number, data: BoardUpdateRequest) =>
    api.put<BoardDetail>(`/board/${id}`, data),

  /**
   * Delete board
   */
  delete: (id: number) =>
    api.delete<void>(`/board/${id}`),

  /**
   * Toggle like on board
   */
  toggleLike: (id: number) =>
    api.post<LikeResponse>(`/board/${id}/like`),
};

// ==================== Comment Endpoints ====================

export const commentEndpoints = {
  /**
   * Get comments for a board
   */
  list: (boardId: number, params: { page?: number; size?: number } = {}) => {
    const { page = 0, size = 20 } = params;
    const queryParams = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    return api.get<CommentPageResponse>(`/board/${boardId}/comments?${queryParams}`);
  },

  /**
   * Create new comment
   */
  create: (boardId: number, data: CommentCreateRequest) =>
    api.post<Comment>(`/board/${boardId}/comments`, data),

  /**
   * Update comment
   */
  update: (commentId: number, data: CommentUpdateRequest) =>
    api.put<Comment>(`/comments/${commentId}`, data),

  /**
   * Delete comment
   */
  delete: (commentId: number) =>
    api.delete<void>(`/comments/${commentId}`),
};

// ==================== Export Combined API ====================

export const apiEndpoints = {
  auth: authEndpoints,
  board: boardEndpoints,
  comment: commentEndpoints,
};

export default apiEndpoints;
