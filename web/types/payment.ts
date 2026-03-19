// 支付相关类型

export type PaymentMethod = "alipay" | "wechat" | "wallet";

export interface CreatePaymentRequest {
  orderId: number;
  paymentMethod: PaymentMethod;
}

export interface Payment {
  id: number;
  userId: number;
  amount: number;
  paymentMethod: PaymentMethod;
  status: number; // 0: pending, 1: success, 2: failed
  orderId: number;
  transactionId: string;
  createdAt: string;
  paidAt?: string;
}

// 钱包支付响应
export interface WalletPaymentResponse {
  paymentId: string;
  amount: number;
  status: number;
  printJobId?: number;
  printSuccess?: boolean;
  printMessage?: string;
}

// 第三方支付响应
export interface ThirdPartyPaymentResponse {
  paymentId: string;
  tradeNo: string;
  amount: number;
  status: number;
  payurl: string;
  qrcode?: string;
}

// 创建支付响应（联合类型）
export type CreatePaymentResponse = WalletPaymentResponse | ThirdPartyPaymentResponse;

// 查询支付状态响应
export interface QueryPaymentResponse {
  paymentId: string;
  status: number;
  amount?: number;
  tradeStatus?: string;
  message: string;
  paymentType?: "order" | "wallet";
  // 订单相关字段
  orderId?: number;
  printerName?: string;
  copies?: number;
  colorMode?: number;
  duplex?: number;
  paperSize?: string;
  finalAmount?: number;
  // 打印相关字段
  printSuccess?: boolean;
  printMessage?: string;
  printJobId?: number;
  // 钱包相关字段
  balance?: number;
}