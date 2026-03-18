// 用户信息状态管理

import { defineStore } from "pinia";
import type { User } from "../types/user";
import { useUserApi } from "../api/user";

interface UserState {
  user: User | null;
  balance: number;
}

const USER_KEY = "user_data";

// SSR-safe localStorage helpers
const getUserFromStorage = (): { user: User | null; balance: number } | null => {
  if (import.meta.client) {
    const stored = localStorage.getItem(USER_KEY);
    return stored ? JSON.parse(stored) : null;
  }
  return null;
};

const setUserToStorage = (data: { user: User | null; balance: number }) => {
  if (import.meta.client) {
    localStorage.setItem(USER_KEY, JSON.stringify(data));
  }
};

const removeUserFromStorage = () => {
  if (import.meta.client) {
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
    isAdmin: (state) => state.user?.role === "admin",
    username: (state) => state.user?.username || "",
  },

  actions: {
    init() {
      // Load user data from localStorage on client side
      const stored = getUserFromStorage();
      if (stored) {
        this.user = stored.user;
        this.balance = stored.balance;
      }
    },

    setUser(user: User) {
      this.user = user;
      this.balance = user.balance;
      setUserToStorage({ user: this.user, balance: this.balance });
    },

    updateBalance(balance: number) {
      this.balance = balance;
      if (this.user) {
        this.user.balance = balance;
      }
      setUserToStorage({ user: this.user, balance: this.balance });
    },

    clearUser() {
      this.user = null;
      this.balance = 0;
      removeUserFromStorage();
    },

    persist() {
      setUserToStorage({ user: this.user, balance: this.balance });
    },

    async fetchProfile() {
      const userApi = useUserApi();

      try {
        const user = await userApi.getProfile();
        this.setUser(user);
        return { success: true, user };
      } catch (error) {
        return {
          success: false,
          message: (error as { message?: string })?.message || "获取用户信息失败",
        };
      }
    },

    async fetchBalance() {
      const userApi = useUserApi();

      try {
        const response = await userApi.getWalletBalance();
        this.updateBalance(response.balance);
        return { success: true, balance: response.balance };
      } catch (error) {
        return {
          success: false,
          message: (error as { message?: string })?.message || "获取余额失败",
        };
      }
    },
  },
});

export type UserStore = ReturnType<typeof useUserStore>;