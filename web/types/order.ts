// 订单相关类型

export type OrderStatus = "pending" | "printing" | "completed" | "cancelled" | "failed";

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
  status: number; // 0: pending, 1: printing, 2: completed, 3: cancelled, 4: failed
  createdAt: string;
  updatedAt: string;
  username: string | null;
}

// 状态映射
export const ORDER_STATUS_MAP: Record<number, OrderStatus> = {
  0: "pending",
  1: "printing",
  2: "completed",
  3: "cancelled",
  4: "failed",
};

export const STATUS_TO_NUMBER_MAP: Record<OrderStatus, number> = {
  pending: 0,
  printing: 1,
  completed: 2,
  cancelled: 3,
  failed: 4,
};

export const COLOR_MODE_MAP: Record<number, "bw" | "color"> = {
  0: "bw",
  1: "color",
};

export interface CreateOrderRequest {
  file_id: string;
  printer_name: string;
  copies: number;
  color_mode: "bw" | "color";
  duplex: boolean;
  paper_size: string;
  page_range?: string;
}

export interface EstimatePriceRequest {
  file_id: string;
  color_mode: "bw" | "color";
  duplex: boolean;
  paper_size: string;
  copies: number;
  page_range?: string;
}

export interface OrderListParams {
  page?: number;
  page_size?: number;
  status?: OrderStatus;
}

export interface CancelOrderResponse {
  success: boolean;
  message: string;
  refund_amount?: number;
}