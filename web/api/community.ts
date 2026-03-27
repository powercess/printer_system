// 社区相关 API

import type {
  CommunityPost,
  CreateShareRequest,
  ShareListParams,
  LikeResponse,
} from "../types/community";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Community");

export const useCommunityApi = () => {
  const { get, post, delete: del } = useApiRequest();

  return {
    // 发布分享
    share: (data: CreateShareRequest) => {
      apiLog.requestStart("POST", "/api/community/share", {
        fileId: data.fileId,
      });
      return post<CommunityPost>("/api/community/share", data);
    },

    // 获取分享列表
    getList: (params?: ShareListParams) => {
      apiLog.requestStart("GET", "/api/community/list", params);
      return get<PaginatedResponse<CommunityPost>>("/api/community/list", params);
    },

    // 获取我的分享
    getMyShares: (params?: ShareListParams) => {
      apiLog.requestStart("GET", "/api/community/my", params);
      return get<PaginatedResponse<CommunityPost>>("/api/community/my", params);
    },

    // 获取分享详情
    getDetail: (postId: string) => {
      apiLog.requestStart("GET", "/api/community/detail", { postId });
      return get<CommunityPost>("/api/community/detail", { postId });
    },

    // 点赞
    like: (shareId: string) => {
      apiLog.requestStart("POST", "/api/community/like", { shareId });
      return post<LikeResponse>(`/api/community/like?shareId=${shareId}`);
    },

    // 取消点赞
    unlike: (shareId: string) => {
      apiLog.requestStart("DELETE", "/api/community/unlike", { shareId });
      return del<LikeResponse>("/api/community/unlike", { shareId });
    },

    // 删除分享
    delete: (shareId: string) => {
      apiLog.requestStart("DELETE", "/api/community/delete", { shareId });
      return del<{ success: boolean }>("/api/community/delete", { shareId });
    },
  };
};