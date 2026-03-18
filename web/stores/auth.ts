// 认证状态管理

import { defineStore } from "pinia";
import type { User } from "../types/user";
import { useUserApi } from "../api/user";
import { useUserStore } from "./user";
import { createStoreLogger } from "../utils/logger";

const log = createStoreLogger("auth");

interface AuthState {
  token: string | null;
  isAuthenticated: boolean;
}

const TOKEN_KEY = "auth_token";

// SSR-safe localStorage helpers
const getTokenFromStorage = (): string | null => {
  if (import.meta.client) {
    const token = localStorage.getItem(TOKEN_KEY);
    log.stateChange("token from storage", null, token ? "存在" : "不存在");
    return token;
  }
  return null;
};

const setTokenToStorage = (token: string) => {
  if (import.meta.client) {
    log.stateChange("token to storage", "不存在", "存在");
    localStorage.setItem(TOKEN_KEY, token);
  }
};

const removeTokenFromStorage = () => {
  if (import.meta.client) {
    log.stateChange("token from storage", "存在", "不存在");
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
      log.init("开始初始化认证状态");
      // Load token from localStorage on client side
      const storedToken = getTokenFromStorage();
      if (storedToken) {
        this.token = storedToken;
        this.isAuthenticated = true;
        log.actionSuccess("init", "已从本地存储恢复认证状态");
      } else {
        log.actionSuccess("init", "无本地存储的认证状态");
      }
    },

    setToken(token: string) {
      log.actionStart("setToken", "设置新 token");
      const oldToken = this.token;
      this.token = token;
      this.isAuthenticated = true;
      setTokenToStorage(token);
      log.stateChange("token", oldToken ? "存在" : "不存在", "存在");
      log.stateChange("isAuthenticated", false, true);
      log.actionSuccess("setToken");
    },

    clearAuth() {
      log.actionStart("clearAuth", "清除认证状态");
      const hadToken = !!this.token;
      this.token = null;
      this.isAuthenticated = false;
      removeTokenFromStorage();
      log.stateChange("token", hadToken ? "存在" : "不存在", "不存在");
      log.stateChange("isAuthenticated", true, false);
      log.actionSuccess("clearAuth");
    },

    async login(username: string, password: string) {
      log.actionStart("login", { username });
      const userApi = useUserApi();
      const userStore = useUserStore();

      try {
        log.trace("发起登录请求");
        const response = await userApi.login({ username, password });
        this.setToken(response.token);
        log.trace("登录成功，获取用户信息");
        // 登录成功后获取用户信息
        await userStore.fetchProfile();
        log.actionSuccess("login", { username });
        return { success: true };
      } catch (error) {
        log.actionError("login", error);
        return {
          success: false,
          message: (error as { message?: string })?.message || "登录失败",
        };
      }
    },

    async register(username: string, password: string, email: string) {
      log.actionStart("register", { username, email });
      const userApi = useUserApi();

      try {
        log.trace("发起注册请求");
        await userApi.register({ username, password, email });
        log.trace("注册成功，自动登录");
        const result = await this.login(username, password);
        log.actionSuccess("register", { username });
        return result;
      } catch (error) {
        log.actionError("register", error);
        return {
          success: false,
          message: (error as { message?: string })?.message || "注册失败",
        };
      }
    },

    logout() {
      log.actionStart("logout");
      this.clearAuth();
      const userStore = useUserStore();
      userStore.clearUser();
      log.actionSuccess("logout", "已清除认证状态和用户数据");
      navigateTo("/login");
    },
  },
});

export type AuthStore = ReturnType<typeof useAuthStore>;