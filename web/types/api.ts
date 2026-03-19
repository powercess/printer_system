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
  fileId: number;
  name: string;
  fileType: string;
  fileSize: number;
  pageCount: number;
  uploadTime: string;
  filePath: string;
}

// 价格估算响应
export interface PriceEstimate {
  originalAmount: number;
  discountAmount: number;
  finalAmount: number;
  pageCount: number;
}