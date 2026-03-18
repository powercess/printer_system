// 促销活动相关 API

import type {
  Promotion,
  PromotionListParams,
  ValidatePromotionRequest,
  ValidatePromotionResponse,
} from "../types/promotion";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";

export const usePromotionApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 获取活动列表
    getList: (params?: PromotionListParams) =>
      get<PaginatedResponse<Promotion>>("/api/promotion/list", params),

    // 验证活动
    validate: (data: ValidatePromotionRequest) =>
      post<ValidatePromotionResponse>("/api/promotion/validate", data),
  };
};