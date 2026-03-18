// API 响应基础类型

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T | null;
}

export interface ApiError {
  statusCode: number;
  message: string;
  data: unknown;
}

export interface PaginatedResponse<T> {
  total: number;
  page: number;
  pageSize: number;
  items: T[];
}

export interface PaginationParams {
  page?: number;
  page_size?: number;
}

// 通用文件上传响应
export interface UploadResponse {
  file_id: string;
  file_name: string;
  file_path: string;
  file_size: number;
}

// 价格估算响应
export interface PriceEstimate {
  price: number;
  pages: number;
}