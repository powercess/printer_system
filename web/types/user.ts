// 用户相关类型

// 后端返回的用户数据结构
export interface User {
  id: number;
  username: string;
  nickname: string;
  email: string;
  avatarUrl: string | null;
  groupId: number;
  groupName: string;
  walletBalance?: number; // 管理员列表接口可能返回
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
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
  nickname?: string;
  avatarUrl?: string;
}

export interface WalletBalance {
  balance: number;
}

export interface RechargeRequest {
  amount: number;
  paymentMethod: string;
}

export interface Transaction {
  id: string; // 可能是 "W" + id 或 "P" + id
  amount: number;
  balanceBefore: number | null;
  balanceAfter: number | null;
  type: number; // 0: recharge, 1: consume, 2: refund
  relatedId: string;
  createdAt: string;
  source: "wallet" | "payment"; // 数据来源
  description?: string; // 交易描述
  paymentMethod?: string; // 支付方式（直接支付时）
  orderId?: number; // 订单ID（直接支付时）
  printerName?: string; // 打印机名称（直接支付时）
}

// 交易类型映射
export const TRANSACTION_TYPE_MAP: Record<number, "recharge" | "consume" | "refund"> = {
  0: "recharge",
  1: "consume",
  2: "refund",
};

// 反向映射：字符串转数字
export const TRANSACTION_TYPE_TO_NUMBER: Record<"recharge" | "consume" | "refund", number> = {
  recharge: 0,
  consume: 1,
  refund: 2,
};

export interface TransactionListParams {
  page?: number;
  pageSize?: number;
  type?: number; // 0: recharge, 1: consume, 2: refund
}