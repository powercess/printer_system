// 社区相关 API

import type {
  CommunityPost,
  CreateShareRequest,
  ShareListParams,
  LikeResponse,
} from "../types/community";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";

export const useCommunityApi = () => {
  const { get, post, delete: del } = useApiRequest();

  return {
    // 发布分享
    share: (data: CreateShareRequest) =>
      post<CommunityPost>("/api/community/share", data),

    // 获取分享列表
    getList: (params?: ShareListParams) =>
      get<PaginatedResponse<CommunityPost>>("/api/community/list", params),

    // 获取我的分享
    getMyShares: (params?: ShareListParams) =>
      get<PaginatedResponse<CommunityPost>>("/api/community/my", params),

    // 获取分享详情
    getDetail: (postId: string) =>
      get<CommunityPost>("/api/community/detail", { post_id: postId }),

    // 点赞
    like: (postId: string) =>
      post<LikeResponse>("/api/community/like", { post_id: postId }),

    // 取消点赞
    unlike: (postId: string) =>
      post<LikeResponse>("/api/community/unlike", { post_id: postId }),

    // 删除分享
    delete: (postId: string) =>
      del<{ success: boolean }>("/api/community/delete", { post_id: postId }),
  };
};