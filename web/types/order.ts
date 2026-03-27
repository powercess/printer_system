// 订单相关类型

export type OrderStatus = "pending" | "paid" | "printing" | "completed" | "cancelled" | "failed";

export interface Order {
  id: number;
  userId: number;
  fileId: number;
  fileName: string | null;
  printerId: number | null;
  printerName: string;
  copies: number;
  colorMode: number; // 0: bw, 1: color
  duplex: number; // 0: simplex, 1: duplex
  paperSize: string;
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
  status: number; // 0: pending, 1: paid, 2: printing, 3: completed, 4: cancelled
  createdAt: string;
  updatedAt: string;
  username: string | null;
}

// 状态映射
export const ORDER_STATUS_MAP: Record<number, OrderStatus> = {
  0: "pending",
  1: "paid",
  2: "printing",
  3: "completed",
  4: "cancelled",
  5: "failed",
};

export const STATUS_TO_NUMBER_MAP: Record<OrderStatus, number> = {
  pending: 0,
  paid: 1,
  printing: 2,
  completed: 3,
  cancelled: 4,
  failed: 5,
};

export const COLOR_MODE_MAP: Record<number, "bw" | "color"> = {
  0: "bw",
  1: "color",
};

export interface CreateOrderRequest {
  fileId: number;
  printerName: string;
  colorMode?: number; // 0: bw, 1: color
  duplex?: number; // 0: simplex, 1: duplex
  paperSize?: string;
  copies?: number;
  promotionId?: number;
}

export interface EstimatePriceRequest {
  fileId: number;
  colorMode?: number;
  duplex?: number;
  paperSize?: string;
  copies?: number;
  promotionId?: number;
}

export interface OrderListParams {
  page?: number;
  pageSize?: number;
  status?: number;
}

export interface CancelOrderResponse {
  success: boolean;
  message: string;
  refund_amount?: number;
}

// 创建订单响应
export interface CreateOrderResponse {
  orderId: number;
  orderNo: string;
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
  pageCount: number;
}