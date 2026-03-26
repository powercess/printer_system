// 文件相关 API

import type { FileInfo, FileListParams, FileDetail, PreviewUploadResponse, PreviewStatus, PreviewConfirmResponse } from "../types/file";
import type { UploadResponse, PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";
import { useAuthStore } from "../stores/auth";

const apiLog = createApiLogger("File");

export const useFileApi = () => {
  const { get, post, delete: del, upload } = useApiRequest();
  const config = useRuntimeConfig();
  const baseUrl = config.public.apiBase;
  const authStore = useAuthStore();

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

    // 获取文件下载URL
    getDownloadUrl: (fileId: number) => {
      apiLog.requestStart("GET", "/api/file/download-url", { fileId });
      return get<{ fileId: string; downloadUrl: string }>("/api/file/download-url", { fileId });
    },

    // ==================== 预览相关接口 ====================

    // 上传文件用于预览
    uploadForPreview: (file: File) => {
      apiLog.requestStart("UPLOAD", "/api/file/preview/upload", {
        fileName: file.name,
        fileSize: file.size,
        fileType: file.type,
      });
      return upload<PreviewUploadResponse>("/api/file/preview/upload", file);
    },

    // 获取预览PDF的URL
    getPreviewPdfUrl: (sessionId: string) => {
      const token = authStore.token;
      return `${baseUrl}/api/file/preview/pdf/${sessionId}?satoken=${token}`;
    },

    // 获取预览状态
    getPreviewStatus: (sessionId: string) => {
      apiLog.requestStart("GET", "/api/file/preview/status", { sessionId });
      return get<PreviewStatus>("/api/file/preview/status", { sessionId });
    },

    // 确认预览并保存文件
    confirmPreview: (sessionId: string) => {
      apiLog.requestStart("POST", "/api/file/preview/confirm", { sessionId });
      // 后端接口使用 query 参数，需要拼接到 URL
      return post<PreviewConfirmResponse>(`/api/file/preview/confirm?sessionId=${sessionId}`);
    },

    // 取消预览
    cancelPreview: (sessionId: string) => {
      apiLog.requestStart("DELETE", "/api/file/preview/cancel", { sessionId });
      return del<{ success: boolean }>("/api/file/preview/cancel", { sessionId });
    },
  };
};