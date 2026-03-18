// 促销活动相关 API

import type {
  Promotion,
  PromotionListParams,
  ValidatePromotionRequest,
  ValidatePromotionResponse,
} from "../types/promotion";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Promotion");

export const usePromotionApi = () => {
  const { get, post } = useApiRequest();

  return {
    // 获取活动列表
    getList: (params?: PromotionListParams) => {
      apiLog.requestStart("GET", "/api/promotion/list", params);
      return get<PaginatedResponse<Promotion>>("/api/promotion/list", params);
    },

    // 验证活动
    validate: (data: ValidatePromotionRequest) => {
      apiLog.requestStart("POST", "/api/promotion/validate", { code: data.code });
      return post<ValidatePromotionResponse>("/api/promotion/validate", data);
    },
  };
};