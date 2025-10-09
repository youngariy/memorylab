/**
 * Authentication Context Provider
 * Manages global auth state and provides auth methods
 */

import React, { createContext, useState, useEffect, ReactNode } from 'react';
import api from '../services/api';
import { authEndpoints } from '../services/endpoints';
import type {
  User,
  LoginRequest,
  RegisterRequest,
  SendVerificationCodeRequest,
  VerifyCodeRequest,
} from '../types/api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  sendVerificationCode: (data: SendVerificationCodeRequest) => Promise<void>;
  verifyCode: (data: VerifyCodeRequest) => Promise<void>;
  checkEmailAvailability: (email: string) => Promise<boolean>;
  checkNicknameAvailability: (nickname: string) => Promise<boolean>;
  refreshUser: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user && api.isAuthenticated();
  const isAdmin = !!user && user.roles.includes('ROLE_ADMIN');

  // Fetch current user on mount if token exists
  useEffect(() => {
    const initAuth = async () => {
      if (api.isAuthenticated()) {
        try {
          const userData = await authEndpoints.me();
          setUser(userData);
        } catch (error) {
          console.error('Failed to fetch user:', error);
          // Token might be invalid, clear it
          api.clearTokens();
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authEndpoints.login(credentials);
      api.setTokens(response.accessToken, response.refreshToken);

      // Fetch user data
      const userData = await authEndpoints.me();
      setUser(userData);
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    }
  };

  const register = async (data: RegisterRequest) => {
    try {
      await authEndpoints.register(data);
      // Note: User needs to login after registration
    } catch (error) {
      console.error('Registration failed:', error);
      throw error;
    }
  };

  const logout = () => {
    api.clearTokens();
    setUser(null);
    window.location.href = '/';
  };

  const sendVerificationCode = async (data: SendVerificationCodeRequest) => {
    try {
      await authEndpoints.sendVerificationCode(data);
    } catch (error) {
      console.error('Failed to send verification code:', error);
      throw error;
    }
  };

  const verifyCode = async (data: VerifyCodeRequest) => {
    try {
      await authEndpoints.verifyCode(data);
    } catch (error) {
      console.error('Failed to verify code:', error);
      throw error;
    }
  };

  const checkEmailAvailability = async (email: string): Promise<boolean> => {
    try {
      await authEndpoints.checkEmail(email);
      return true; // Available
    } catch (error) {
      return false; // Not available
    }
  };

  const checkNicknameAvailability = async (nickname: string): Promise<boolean> => {
    try {
      await authEndpoints.checkNickname(nickname);
      return true; // Available
    } catch (error) {
      return false; // Not available
    }
  };

  const refreshUser = async () => {
    if (api.isAuthenticated()) {
      try {
        const userData = await authEndpoints.me();
        setUser(userData);
      } catch (error) {
        console.error('Failed to refresh user:', error);
        throw error;
      }
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated,
        isAdmin,
        isLoading,
        login,
        register,
        logout,
        sendVerificationCode,
        verifyCode,
        checkEmailAvailability,
        checkNicknameAvailability,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
