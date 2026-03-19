// 支付相关类型

export interface CreatePaymentRequest {
  orderId: number;
  paymentMethod: "alipay" | "wechat" | "wallet";
}

export interface Payment {
  id: string;
  userId: number;
  amount: number;
  paymentMethod: "alipay" | "wechat" | "wallet";
  status: "pending" | "success" | "failed" | "cancelled";
  orderId?: number;
  createdAt: string;
  paidAt?: string;
}

export interface PaymentStatus {
  paymentId: string;
  status: Payment["status"];
  qrCodeUrl?: string;
  redirectUrl?: string;
}