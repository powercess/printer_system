// 支付相关 API

import type {
  CreatePaymentRequest,
  Payment,
  PaymentStatus,
} from "../types/payment";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Payment");

export const usePaymentApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 创建支付
    create: (data: CreatePaymentRequest) => {
      apiLog.requestStart("POST", "/api/payment/create", {
        orderId: data.orderId,
        paymentMethod: data.paymentMethod,
      });
      return post<Payment>("/api/payment/create", data);
    },

    // 获取支付状态
    getStatus: (paymentId: string) => {
      apiLog.requestStart("GET", "/api/payment/status", { paymentId });
      return get<PaymentStatus>("/api/payment/status", { paymentId });
    },
  };
};