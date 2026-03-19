// 订单相关 API

import type {
  Order,
  CreateOrderRequest,
  EstimatePriceRequest,
  OrderListParams,
  CancelOrderResponse,
  CreateOrderResponse,
} from "../types/order";
import type { PriceEstimate, PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Order");

export const useOrderApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 创建订单
    create: (data: CreateOrderRequest) => {
      apiLog.requestStart("POST", "/api/order/create", {
        fileId: data.fileId,
        printerName: data.printerName,
        copies: data.copies,
        colorMode: data.colorMode,
        duplex: data.duplex,
        paperSize: data.paperSize,
      });
      return post<CreateOrderResponse>("/api/order/create", data);
    },

    // 估算价格
    estimate: (data: EstimatePriceRequest) => {
      apiLog.requestStart("POST", "/api/order/estimate", {
        fileId: data.fileId,
        colorMode: data.colorMode,
        duplex: data.duplex,
        paperSize: data.paperSize,
        copies: data.copies,
      });
      return post<PriceEstimate>("/api/order/estimate", data);
    },

    // 获取订单列表
    getList: (params?: OrderListParams) => {
      apiLog.requestStart("GET", "/api/order/list", params);
      return get<PaginatedResponse<Order>>("/api/order/list", params);
    },

    // 获取订单详情
    getDetail: (orderId: number) => {
      apiLog.requestStart("GET", "/api/order/detail", { orderId });
      return get<Order>("/api/order/detail", { orderId });
    },

    // 取消订单
    cancel: (orderId: number) => {
      apiLog.requestStart("POST", "/api/order/cancel", { orderId });
      return post<CancelOrderResponse>(`/api/order/cancel?orderId=${orderId}`);
    },
  };
};