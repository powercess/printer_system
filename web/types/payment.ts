// 支付相关类型

export interface CreatePaymentRequest {
  amount: number;
  payment_method: "alipay" | "wechat" | "balance";
  order_id?: string;
}

export interface Payment {
  id: string;
  user_id: number;
  amount: number;
  payment_method: "alipay" | "wechat" | "balance";
  status: "pending" | "success" | "failed" | "cancelled";
  order_id?: string;
  created_at: string;
  paid_at?: string;
}

export interface PaymentStatus {
  payment_id: string;
  status: Payment["status"];
  qr_code_url?: string;
  redirect_url?: string;
}