// 订单相关类型

export interface Order {
  id: string;
  user_id: number;
  file_id: string;
  file_name: string;
  printer_name: string;
  copies: number;
  color_mode: "bw" | "color";
  duplex: boolean;
  paper_size: string;
  page_range: string;
  total_pages: number;
  price: number;
  status: "pending" | "printing" | "completed" | "cancelled" | "failed";
  created_at: string;
  updated_at: string;
}

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
  status?: Order["status"];
}

export interface CancelOrderResponse {
  success: boolean;
  message: string;
  refund_amount?: number;
}