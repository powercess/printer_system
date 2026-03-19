// 支付相关 API

import type {
  CreatePaymentRequest,
  CreatePaymentResponse,
  Payment,
  QueryPaymentResponse,
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
      return post<CreatePaymentResponse>("/api/payment/create", data);
    },

    // 获取支付状态
    getStatus: (paymentId: string) => {
      apiLog.requestStart("GET", "/api/payment/status", { paymentId });
      return get<Payment>("/api/payment/status", { paymentId });
    },

    // 查询支付状态并处理订单
    query: (outTradeNo: string) => {
      apiLog.requestStart("GET", "/api/payment/query", { outTradeNo });
      return get<QueryPaymentResponse>("/api/payment/query", { outTradeNo });
    },
  };
};