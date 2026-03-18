// 支付相关 API

import type {
  CreatePaymentRequest,
  Payment,
  PaymentStatus,
} from "../types/payment";
import { useApiRequest } from "./index";

export const usePaymentApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 创建支付
    create: (data: CreatePaymentRequest) =>
      post<Payment>("/api/payment/create", data),

    // 获取支付状态
    getStatus: (paymentId: string) =>
      get<PaymentStatus>("/api/payment/status", { payment_id: paymentId }),
  };
};