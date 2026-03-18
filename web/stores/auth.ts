// 认证状态管理

import { defineStore } from "pinia";
import type { User } from "../types/user";
import { useUserApi } from "../api/user";

interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
}

const TOKEN_KEY = "auth_token";

// SSR-safe localStorage helpers
const getTokenFromStorage = (): string | null => {
  if (import.meta.client) {
    return localStorage.getItem(TOKEN_KEY);
  }
  return null;
};

const setTokenToStorage = (token: string) => {
  if (import.meta.client) {
    localStorage.setItem(TOKEN_KEY, token);
  }
};

const removeTokenFromStorage = () => {
  if (import.meta.client) {
    localStorage.removeItem(TOKEN_KEY);
  }
};

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: null,
    isAuthenticated: false,
  }),

  getters: {
    getToken: (state) => state.token,
    isLoggedIn: (state) => state.isAuthenticated && !!state.token,
  },

  actions: {
    init() {
      // Load token from localStorage on client side
      const storedToken = getTokenFromStorage();
      if (storedToken) {
        this.token = storedToken;
        this.isAuthenticated = true;
      }
    },

    setToken(token: string) {
      this.token = token;
      this.isAuthenticated = true;
      setTokenToStorage(token);
    },

    clearAuth() {
      this.token = null;
      this.isAuthenticated = false;
      removeTokenFromStorage();
    },

    async login(username: string, password: string) {
      const userApi = useUserApi();
      const userStore = useUserStore();

      try {
        const response = await userApi.login({ username, password });
        this.setToken(response.token);
        // 登录成功后获取用户信息
        await userStore.fetchProfile();
        return { success: true };
      } catch (error) {
        return {
          success: false,
          message: (error as { message?: string })?.message || "登录失败",
        };
      }
    },

    async register(username: string, password: string, email: string) {
      const userApi = useUserApi();

      try {
        await userApi.register({ username, password, email });
        return await this.login(username, password);
      } catch (error) {
        return {
          success: false,
          message: (error as { message?: string })?.message || "注册失败",
        };
      }
    },

    logout() {
      this.clearAuth();
      const userStore = useUserStore();
      userStore.clearUser();
      navigateTo("/login");
    },
  },
});

export type AuthStore = ReturnType<typeof useAuthStore>;