/**
 * API Client for MemoryLab Backend
 * Uses relative /api paths for Nginx proxy compatibility
 */

const API_BASE = '/api';

interface RequestConfig extends RequestInit {
  skipAuth?: boolean;
}

class ApiClient {
  private getAuthToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  private setAuthToken(token: string): void {
    localStorage.setItem('accessToken', token);
  }

  private removeAuthToken(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
  }

  private async refreshAccessToken(): Promise<string | null> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return null;

    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (response.ok) {
        const data = await response.json();
        this.setAuthToken(data.accessToken);
        return data.accessToken;
      }
    } catch (error) {
      console.error('Failed to refresh token:', error);
    }

    return null;
  }

  async request<T>(
    endpoint: string,
    config: RequestConfig = {}
  ): Promise<T> {
    const { skipAuth = false, ...restConfig } = config;

    const headers: Record<string, string> = {
      ...(restConfig.headers as Record<string, string>),
    };

    // Add Authorization header if token exists and not skipped
    if (!skipAuth) {
      const token = this.getAuthToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
    }

    // Add Content-Type for non-FormData requests
    if (!(restConfig.body instanceof FormData) && restConfig.body) {
      headers['Content-Type'] = 'application/json';
    }

    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE}${endpoint}`;

    try {
      let response = await fetch(url, {
        ...restConfig,
        headers,
      });

      // Handle 401 Unauthorized - try to refresh token
      if (response.status === 401 && !skipAuth && !endpoint.includes('/auth/refresh')) {
        const newToken = await this.refreshAccessToken();

        if (newToken) {
          // Retry the original request with new token
          headers['Authorization'] = `Bearer ${newToken}`;
          response = await fetch(url, {
            ...restConfig,
            headers,
          });
        } else {
          // Refresh failed, redirect to login
          this.removeAuthToken();
          window.location.href = '/login';
          throw new Error('Session expired. Please login again.');
        }
      }

      // Handle non-2xx responses
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: response.statusText }));
        throw new Error(errorData.message || `HTTP Error ${response.status}`);
      }

      // Handle 204 No Content
      if (response.status === 204) {
        return null as T;
      }

      return await response.json();
    } catch (error) {
      console.error('API Request failed:', error);
      throw error;
    }
  }

  // Convenience methods
  async get<T>(endpoint: string, config?: RequestConfig): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: 'GET' });
  }

  async post<T>(endpoint: string, data?: any, config?: RequestConfig): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'POST',
      body: data instanceof FormData ? data : JSON.stringify(data),
    });
  }

  async put<T>(endpoint: string, data?: any, config?: RequestConfig): Promise<T> {
    return this.request<T>(endpoint, {
      ...config,
      method: 'PUT',
      body: data instanceof FormData ? data : JSON.stringify(data),
    });
  }

  async delete<T>(endpoint: string, config?: RequestConfig): Promise<T> {
    return this.request<T>(endpoint, { ...config, method: 'DELETE' });
  }

  // Auth helpers
  setTokens(accessToken: string, refreshToken: string): void {
    this.setAuthToken(accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  }

  clearTokens(): void {
    this.removeAuthToken();
  }

  isAuthenticated(): boolean {
    return !!this.getAuthToken();
  }
}

export const api = new ApiClient();
export default api;
