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
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Order");

export const useOrderApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 创建订单
    create: (data: CreateOrderRequest) => {
      apiLog.requestStart("POST", "/api/order/create", {
        fileId: data.file_id,
        printerName: data.printer_name,
        copies: data.copies,
        colorMode: data.color_mode,
        duplex: data.duplex,
        paperSize: data.paper_size,
        pageRange: data.page_range || "全部",
      });
      return post<Order>("/api/order/create", data);
    },

    // 估算价格
    estimate: (data: EstimatePriceRequest) => {
      apiLog.requestStart("POST", "/api/order/estimate", {
        fileId: data.file_id,
        colorMode: data.color_mode,
        duplex: data.duplex,
        paperSize: data.paper_size,
        copies: data.copies,
        pageRange: data.page_range || "全部",
      });
      return post<PriceEstimate>("/api/order/estimate", data);
    },

    // 获取订单列表
    getList: (params?: OrderListParams) => {
      apiLog.requestStart("GET", "/api/order/list", params);
      return get<PaginatedResponse<Order>>("/api/order/list", params);
    },

    // 获取订单详情
    getDetail: (orderId: string) => {
      apiLog.requestStart("GET", "/api/order/detail", { orderId });
      return get<Order>("/api/order/detail", { order_id: orderId });
    },

    // 取消订单
    cancel: (orderId: string) => {
      apiLog.requestStart("POST", "/api/order/cancel", { orderId });
      return post<CancelOrderResponse>("/api/order/cancel", { order_id: orderId });
    },
  };
};