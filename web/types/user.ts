// 用户相关类型

export interface User {
  id: number;
  username: string;
  email: string;
  role: "user" | "admin";
  balance: number;
  created_at: string;
  updated_at: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
}

export interface LoginResponse {
  token: string;
}

export interface UpdateProfileRequest {
  email?: string;
  password?: string;
}

export interface WalletBalance {
  balance: number;
}

export interface RechargeRequest {
  amount: number;
  payment_method: string;
}

export interface Transaction {
  id: number;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  type: number; // 0: recharge, 1: consume, 2: refund
  relatedId: string;
  createdAt: string;
}

// 交易类型映射
export const TRANSACTION_TYPE_MAP: Record<number, "recharge" | "consume" | "refund"> = {
  0: "recharge",
  1: "consume",
  2: "refund",
};

export interface TransactionListParams {
  page?: number;
  page_size?: number;
  type?: "recharge" | "consume" | "refund";
}