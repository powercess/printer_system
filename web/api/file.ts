// 文件相关 API

import type { FileInfo, FileListParams, FileDetail } from "../types/file";
import type { UploadResponse, PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";

export const useFileApi = () => {
  const { get, delete: del, upload } = useApiRequest();

  return {
    // 上传文件
    upload: (file: File) =>
      upload<UploadResponse>("/api/file/upload", file),

    // 获取文件列表
    getList: (params?: FileListParams) =>
      get<PaginatedResponse<FileInfo>>("/api/file/list", params),

    // 获取文件详情
    getDetail: (fileId: string) =>
      get<FileDetail>("/api/file/detail", { file_id: fileId }),

    // 删除文件
    delete: (fileId: string) =>
      del<{ success: boolean }>("/api/file/delete", { file_id: fileId }),
  };
};