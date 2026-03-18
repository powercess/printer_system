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

export const useUserApi = () => {
  const { post, get, put } = useApiRequest();

  return {
    // 用户注册
    register: (data: RegisterRequest) =>
      post<User>("/api/user/register", data),

    // 用户登录
    login: (data: LoginRequest) =>
      post<LoginResponse>("/api/user/login", data),

    // 获取用户信息
    getProfile: () => get<User>("/api/user/profile"),

    // 更新用户信息
    updateProfile: (data: UpdateProfileRequest) =>
      put<User>("/api/user/profile", data),

    // 获取钱包余额
    getWalletBalance: () => get<WalletBalance>("/api/user/wallet/balance"),

    // 钱包充值
    recharge: (data: RechargeRequest) =>
      post<Transaction>("/api/user/wallet/recharge", data),

    // 获取钱包流水
    getTransactions: (params?: TransactionListParams) =>
      get<PaginatedResponse<Transaction>>("/api/user/wallet/transactions", params),
  };
};