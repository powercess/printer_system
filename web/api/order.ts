// 订单相关 API

import type {
  Order,
  CreateOrderRequest,
  EstimatePriceRequest,
  OrderListParams,
  CancelOrderResponse,
} from "../types/order";
import type { PriceEstimate, PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";

export const useOrderApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 创建订单
    create: (data: CreateOrderRequest) =>
      post<Order>("/api/order/create", data),

    // 估算价格
    estimate: (data: EstimatePriceRequest) =>
      post<PriceEstimate>("/api/order/estimate", data),

    // 获取订单列表
    getList: (params?: OrderListParams) =>
      get<PaginatedResponse<Order>>("/api/order/list", params),

    // 获取订单详情
    getDetail: (orderId: string) =>
      get<Order>("/api/order/detail", { order_id: orderId }),

    // 取消订单
    cancel: (orderId: string) =>
      post<CancelOrderResponse>("/api/order/cancel", { order_id: orderId }),
  };
};