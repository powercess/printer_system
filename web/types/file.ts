// 文件相关类型

export interface FileInfo {
  id: number;
  userId: number;
  name: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  pageCount: number;
  uploadTime: string;
  deletedAt: string | null;
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
  id: number;
  userId: number;
  name: string;
  filePath: string;
  fileSize: number;
  fileType: string;
  pageCount: number;
  uploadTime: string;
  deletedAt: string | null;
}