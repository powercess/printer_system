// 用户信息状态管理

import { defineStore } from "pinia";
import type { User } from "../types/user";
import { useUserApi } from "../api/user";
import { createStoreLogger } from "../utils/logger";

const log = createStoreLogger("user");

interface UserState {
  user: User | null;
  balance: number;
}

const USER_KEY = "user_data";

// SSR-safe localStorage helpers
const getUserFromStorage = (): { user: User | null; balance: number } | null => {
  if (import.meta.client) {
    const stored = localStorage.getItem(USER_KEY);
    const result = stored ? JSON.parse(stored) : null;
    log.stateChange("user from storage", null, result ? "存在" : "不存在");
    return result;
  }
  return null;
};

const setUserToStorage = (data: { user: User | null; balance: number }) => {
  if (import.meta.client) {
    log.stateChange("user to storage", "不存在", "存在");
    localStorage.setItem(USER_KEY, JSON.stringify(data));
  }
};

const removeUserFromStorage = () => {
  if (import.meta.client) {
    log.stateChange("user from storage", "存在", "不存在");
    localStorage.removeItem(USER_KEY);
  }
};

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    user: null,
    balance: 0,
  }),

  getters: {
    getUser: (state) => state.user,
    getBalance: (state) => state.balance,
    // 根据 groupId 判断是否为管理员 (假设 groupId 为特定值为管理员)
    isAdmin: (state) => state.user?.groupId === 0 || state.user?.groupName === "管理员",
    username: (state) => state.user?.nickname || state.user?.username || "",
  },

  actions: {
    init() {
      log.init("开始初始化用户状态");
      // Load user data from localStorage on client side
      const stored = getUserFromStorage();
      if (stored) {
        this.user = stored.user;
        this.balance = stored.balance;
        log.actionSuccess("init", {
          username: stored.user?.nickname || stored.user?.username,
          balance: stored.balance,
        });
      } else {
        log.actionSuccess("init", "无本地存储的用户数据");
      }
    },

    setUser(user: User, balance?: number) {
      log.actionStart("setUser", { username: user.username, nickname: user.nickname, groupId: user.groupId });
      const oldUser = this.user?.username;
      this.user = user;
      // balance 需要单独获取，如果传入了则使用，否则保持不变
      if (balance !== undefined) {
        this.balance = balance;
      }
      setUserToStorage({ user: this.user, balance: this.balance });
      log.stateChange("user", oldUser || "不存在", user.username);
      log.actionSuccess("setUser");
    },

    updateBalance(balance: number) {
      log.actionStart("updateBalance", { newBalance: balance });
      const oldBalance = this.balance;
      this.balance = balance;
      // 同步更新 user 对象中的 walletBalance
      if (this.user) {
        this.user = { ...this.user, walletBalance: balance };
      }
      setUserToStorage({ user: this.user, balance: this.balance });
      log.stateChange("balance", oldBalance, balance);
      log.actionSuccess("updateBalance");
    },

    clearUser() {
      log.actionStart("clearUser");
      const hadUser = !!this.user;
      this.user = null;
      this.balance = 0;
      removeUserFromStorage();
      log.stateChange("user", hadUser ? "存在" : "不存在", "不存在");
      log.stateChange("balance", this.balance, 0);
      log.actionSuccess("clearUser");
    },

    persist() {
      log.trace("持久化用户数据");
      setUserToStorage({ user: this.user, balance: this.balance });
    },

    async fetchProfile() {
      log.actionStart("fetchProfile");
      const userApi = useUserApi();

      try {
        log.trace("发起获取用户信息请求");
        const user = await userApi.getProfile();
        // 先设置用户信息，然后获取余额
        this.setUser(user);
        // 单独获取余额
        await this.fetchBalance();
        log.actionSuccess("fetchProfile", {
          username: user.nickname || user.username,
          groupId: user.groupId,
          groupName: user.groupName,
          balance: this.balance,
        });
        return { success: true, user };
      } catch (error) {
        log.actionError("fetchProfile", error);
        return {
          success: false,
          message: (error as { message?: string })?.message || "获取用户信息失败",
        };
      }
    },

    async fetchBalance() {
      log.actionStart("fetchBalance");
      const userApi = useUserApi();

      try {
        log.trace("发起获取余额请求");
        const response = await userApi.getWalletBalance();
        this.updateBalance(response.balance);
        log.actionSuccess("fetchBalance", { balance: response.balance });
        return { success: true, balance: response.balance };
      } catch (error) {
        log.actionError("fetchBalance", error);
        return {
          success: false,
          message: (error as { message?: string })?.message || "获取余额失败",
        };
      }
    },
  },
});

export type UserStore = ReturnType<typeof useUserStore>;