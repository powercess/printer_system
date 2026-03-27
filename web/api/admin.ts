// 管理员相关 API

import type { User, UpdateProfileRequest } from "../types/user";
import type { FileInfo } from "../types/file";
import type { Order } from "../types/order";
import type { PaginatedResponse } from "../types/api";
import { useApiRequest } from "./index";
import { createApiLogger } from "../utils/logger";

const apiLog = createApiLogger("Admin");

interface AdminStats {
  total_users: number;
  total_orders: number;
  total_revenue: number;
  today_orders: number;
  today_revenue: number;
}

interface AdminUserListParams {
  page?: number;
  pageSize?: number;
  role?: "user" | "admin";
  search?: string;
}

interface AdminFileListParams {
  page?: number;
  pageSize?: number;
  user_id?: number;
}

interface AdminOrderListParams {
  page?: number;
  pageSize?: number;
  status?: Order["status"];
  user_id?: number;
}

export const useAdminApi = () => {
  const { get, post, put, delete: del } = useApiRequest();

  return {
    // 创建用户
    createUser: (data: { username: string; password: string; email: string; role?: "user" | "admin" }) => {
      apiLog.requestStart("POST", "/api/admin/user/create", {
        username: data.username,
        email: data.email,
        role: data.role || "user",
      });
      return post<User>("/api/admin/user/create", data);
    },

    // 获取用户列表
    getUserList: (params?: AdminUserListParams) => {
      apiLog.requestStart("GET", "/api/admin/user/list", params);
      return get<PaginatedResponse<User>>("/api/admin/user/list", params);
    },

    // 更新用户
    updateUser: (data: { user_id: number } & UpdateProfileRequest) => {
      apiLog.requestStart("PUT", "/api/admin/user/update", {
        userId: data.user_id,
        fields: Object.keys(data),
      });
      return put<User>("/api/admin/user/update", data);
    },

    // 删除用户
    deleteUser: (userId: number) => {
      apiLog.requestStart("DELETE", "/api/admin/user/delete", { userId });
      return del<{ success: boolean }>("/api/admin/user/delete", { user_id: userId });
    },

    // 获取文件列表
    getFileList: (params?: AdminFileListParams) => {
      apiLog.requestStart("GET", "/api/admin/file/list", params);
      return get<PaginatedResponse<FileInfo>>("/api/admin/file/list", params);
    },

    // 获取订单列表
    getOrderList: (params?: AdminOrderListParams) => {
      apiLog.requestStart("GET", "/api/admin/order/list", params);
      return get<PaginatedResponse<Order>>("/api/admin/order/list", params);
    },

    // 获取统计数据
    getStats: () => {
      apiLog.requestStart("GET", "/api/admin/stats");
      return get<AdminStats>("/api/admin/stats");
    },
  };
};