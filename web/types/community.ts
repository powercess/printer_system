// 社区相关类型

export interface CommunityPost {
  id: string;
  user_id: number;
  username: string;
  title: string;
  content: string;
  file_id?: string;
  file_name?: string;
  likes_count: number;
  is_liked: boolean;
  created_at: string;
  updated_at: string;
}

export interface CreateShareRequest {
  title: string;
  content: string;
  file_id?: string;
}

export interface ShareListParams {
  page?: number;
  page_size?: number;
}

export interface LikeResponse {
  success: boolean;
  likes_count: number;
}