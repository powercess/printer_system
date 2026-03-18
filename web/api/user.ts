// 用户相关 API

import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  User,
  UpdateProfileRequest,
  WalletBalance,
  RechargeRequest,
  Transaction,
  TransactionListParams,
} from "../types/user";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("User");

export const useUserApi = () => {
  const { post, get, put } = useApiRequest();

  return {
    // 用户注册
    register: (data: RegisterRequest) => {
      apiLog.requestStart("POST", "/api/user/register", { username: data.username, email: data.email });
      return post<User>("/api/user/register", data);
    },

    // 用户登录
    login: (data: LoginRequest) => {
      apiLog.requestStart("POST", "/api/user/login", { username: data.username });
      return post<LoginResponse>("/api/user/login", data);
    },

    // 获取用户信息
    getProfile: () => {
      apiLog.requestStart("GET", "/api/user/profile");
      return get<User>("/api/user/profile");
    },

    // 更新用户信息
    updateProfile: (data: UpdateProfileRequest) => {
      apiLog.requestStart("PUT", "/api/user/profile", data);
      return put<User>("/api/user/profile", data);
    },

    // 获取钱包余额
    getWalletBalance: () => {
      apiLog.requestStart("GET", "/api/user/wallet/balance");
      return get<WalletBalance>("/api/user/wallet/balance");
    },

    // 钱包充值
    recharge: (data: RechargeRequest) => {
      apiLog.requestStart("POST", "/api/user/wallet/recharge", { amount: data.amount, paymentMethod: data.payment_method });
      return post<Transaction>("/api/user/wallet/recharge", data);
    },

    // 获取钱包流水
    getTransactions: (params?: TransactionListParams) => {
      apiLog.requestStart("GET", "/api/user/wallet/transactions", params);
      return get<PaginatedResponse<Transaction>>("/api/user/wallet/transactions", params);
    },
  };
};