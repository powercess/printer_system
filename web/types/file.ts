// 文件相关类型

export interface FileInfo {
  id: number;
  userId: number;
  blobId: number;
  displayName: string;
  pageCount: number;
  uploadTime: string;
  createdAt: string;
  deletedAt: string | null;
  // 从 blob 关联的字段
  fileSize: number;
  fileType: string;
  storagePath: string;
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
  blobId: number;
  displayName: string;
  pageCount: number;
  uploadTime: string;
  createdAt: string;
  deletedAt: string | null;
  fileSize: number;
  fileType: string;
  storagePath: string;
}