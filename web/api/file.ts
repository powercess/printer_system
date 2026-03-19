// 文件相关 API

import type { FileInfo, FileListParams, FileDetail } from "../types/file";
import type { UploadResponse, PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("File");

export const useFileApi = () => {
  const { get, delete: del, upload } = useApiRequest();

  return {
    // 上传文件
    upload: (file: File) => {
      apiLog.requestStart("UPLOAD", "/api/file/upload", {
        fileName: file.name,
        fileSize: file.size,
        fileType: file.type,
      });
      return upload<UploadResponse>("/api/file/upload", file);
    },

    // 获取文件列表
    getList: (params?: FileListParams) => {
      apiLog.requestStart("GET", "/api/file/list", params);
      return get<PaginatedResponse<FileInfo>>("/api/file/list", params);
    },

    // 获取文件详情
    getDetail: (fileId: number) => {
      apiLog.requestStart("GET", "/api/file/detail", { fileId });
      return get<FileDetail>("/api/file/detail", { fileId });
    },

    // 删除文件
    delete: (fileId: number) => {
      apiLog.requestStart("DELETE", "/api/file/delete", { fileId });
      return del<{ success: boolean }>("/api/file/delete", { fileId });
    },
  };
};