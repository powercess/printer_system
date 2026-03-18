// 文件相关类型

export interface FileInfo {
  id: string;
  user_id: number;
  file_name: string;
  file_path: string;
  file_size: number;
  file_type: string;
  pages: number;
  created_at: string;
  updated_at: string;
}

export interface UploadFileParams {
  file: File;
}

export interface FileListParams {
  page?: number;
  page_size?: number;
  file_type?: string;
}

export interface FileDetail {
  id: string;
  user_id: number;
  file_name: string;
  file_path: string;
  file_size: number;
  file_type: string;
  pages: number;
  created_at: string;
  updated_at: string;
}