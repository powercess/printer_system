// API 基础配置和请求封装

import type { ApiResponse, ApiError } from "../types/api";
import { useAuthStore } from "../stores/auth";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("request");

export const createApiError = (
  statusCode: number,
  message: string,
  data?: unknown,
): ApiError => ({
  statusCode,
  message,
  data,
});

export const useApiRequest = () => {
  const config = useRuntimeConfig();
  const baseUrl = config.public.apiBase;

  const request = async <T>(
    endpoint: string,
    options: RequestInit & {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      params?: any;
    } = {},
  ): Promise<T> => {
    const { params, ...fetchOptions } = options;
    const method = fetchOptions.method || "GET";

    // 构建URL
    let url = `${baseUrl}${endpoint}`;

    // 添加查询参数
    if (params) {
      const searchParams = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          searchParams.append(key, String(value));
        }
      });
      const queryString = searchParams.toString();
      if (queryString) {
        url += `?${queryString}`;
      }
    }

    // 获取token
    const authStore = useAuthStore();
    const token = authStore.token;

    // 设置默认headers
    const headers: HeadersInit = {
      "Content-Type": "application/json",
      ...options.headers,
    };

    // 添加认证token
    if (token) {
      (headers as Record<string, string>)["satoken"] = token;
    }

    // 记录请求开始
    apiLog.requestStart(method, endpoint, params);

    try {
      const response = await fetch(url, {
        ...fetchOptions,
        headers,
      });

      const data: ApiResponse<T> = await response.json();

      // 检查业务逻辑错误
      if (data.code !== 200 && data.code !== 201) {
        // 401 未授权，自动退出登录
        if (data.code === 401) {
          apiLog.requestError(method, endpoint, { code: data.code, message: "未授权，自动退出登录" });
          const authStore = useAuthStore();
          authStore.logout();
          throw createApiError(401, "登录已过期，请重新登录", data.data);
        }

        const error = createApiError(
          data.code,
          data.message || "请求失败",
          data.data,
        );
        apiLog.requestError(method, endpoint, { code: data.code, message: data.message });
        throw error;
      }

      // 记录请求成功
      apiLog.requestSuccess(method, endpoint, data.data);
      return data.data as T;
    } catch (error) {
      if ((error as ApiError).statusCode) {
        throw error;
      }

      // 网络错误或其他错误
      const networkError = createApiError(
        500,
        (error as Error).message || "网络错误",
        null,
      );
      apiLog.requestError(method, endpoint, error);
      throw networkError;
    }
  };

  return {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    get: <T>(endpoint: string, params?: any) =>
      request<T>(endpoint, { method: "GET", params }),

    post: <T>(endpoint: string, body?: unknown) =>
      request<T>(endpoint, {
        method: "POST",
        body: body ? JSON.stringify(body) : undefined,
      }),

    put: <T>(endpoint: string, body?: unknown) =>
      request<T>(endpoint, {
        method: "PUT",
        body: body ? JSON.stringify(body) : undefined,
      }),

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    delete: <T>(endpoint: string, params?: any) =>
      request<T>(endpoint, { method: "DELETE", params }),

    upload: async <T>(
      endpoint: string,
      file: File,
      additionalData?: Record<string, string>,
    ): Promise<T> => {
      apiLog.requestStart("UPLOAD", endpoint, { fileName: file.name, size: file.size });

      const formData = new FormData();
      formData.append("file", file);

      if (additionalData) {
        Object.entries(additionalData).forEach(([key, value]) => {
          formData.append(key, value);
        });
      }

      const authStore = useAuthStore();
      const token = authStore.token;

      try {
        const response = await fetch(`${baseUrl}${endpoint}`, {
          method: "POST",
          headers: token ? { satoken: token } : {},
          body: formData,
        });

        const data: ApiResponse<T> = await response.json();

        if (data.code !== 200 && data.code !== 201) {
          // 401 未授权，自动退出登录
          if (data.code === 401) {
            apiLog.requestError("UPLOAD", endpoint, { code: data.code, message: "未授权，自动退出登录" });
            const authStore = useAuthStore();
            authStore.logout();
            throw createApiError(401, "登录已过期，请重新登录", data.data);
          }

          const error = createApiError(
            data.code,
            data.message || "上传失败",
            data.data,
          );
          apiLog.requestError("UPLOAD", endpoint, { code: data.code, message: data.message });
          throw error;
        }

        apiLog.requestSuccess("UPLOAD", endpoint, data.data);
        return data.data as T;
      } catch (error) {
        if ((error as ApiError).statusCode) {
          throw error;
        }
        apiLog.requestError("UPLOAD", endpoint, error);
        throw createApiError(500, (error as Error).message || "上传失败", null);
      }
    },
  };
};