// 促销活动相关类型

export interface Promotion {
  id: string;
  name: string;
  description: string;
  discount_type: "percentage" | "fixed" | "free";
  discount_value: number;
  min_amount: number;
  max_discount: number;
  start_date: string;
  end_date: string;
  is_active: boolean;
  code?: string;
}

export interface PromotionListParams {
  page?: number;
  pageSize?: number;
  is_active?: boolean;
}

export interface ValidatePromotionRequest {
  code: string;
  amount: number;
}

export interface ValidatePromotionResponse {
  valid: boolean;
  discount: number;
  final_amount: number;
  message?: string;
}