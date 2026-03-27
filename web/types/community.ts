// 社区相关类型

export interface CommunityPost {
  id: string;
  userId: number;
  username: string;
  nickname?: string;
  fileId: string;
  fileName?: string;
  filePath?: string;
  likeCount: number;
  isLiked: boolean;
  createdAt: string;
}

export interface CreateShareRequest {
  fileId: number;
}

export interface ShareListParams {
  page?: number;
  pageSize?: number;
}

export interface LikeResponse {
  success: boolean;
  likeCount: number;
}