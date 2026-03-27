// 管理员相关 API

import type { User } from "../types/user";
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
  active_printers: number;
}

interface AdminUserListParams {
  page?: number;
  pageSize?: number;
  username?: string;
  groupId?: number;
}

interface AdminFileListParams {
  page?: number;
  pageSize?: number;
  userId?: number;
}

interface AdminOrderListParams {
  page?: number;
  pageSize?: number;
  status?: number;
  userId?: number;
}

interface CreateUserData {
  username: string;
  password: string;
  email: string;
  nickname?: string;
  groupId?: number;
}

interface UpdateUserData {
  userId: number;
  nickname?: string;
  email?: string;
  password?: string;
  groupId?: number;
  walletBalance?: number;
}

export const useAdminApi = () => {
  const { get, post, put, delete: del } = useApiRequest();

  return {
    // 创建用户
    createUser: (data: CreateUserData) => {
      apiLog.requestStart("POST", "/api/admin/user/create", {
        username: data.username,
        email: data.email,
        groupId: data.groupId,
      });
      return post<User>("/api/admin/user/create", data);
    },

    // 获取用户列表
    getUserList: (params?: AdminUserListParams) => {
      apiLog.requestStart("GET", "/api/admin/user/list", params);
      return get<PaginatedResponse<User>>("/api/admin/user/list", params);
    },

    // 更新用户
    updateUser: (data: UpdateUserData) => {
      apiLog.requestStart("PUT", "/api/admin/user/update", {
        userId: data.userId,
        fields: Object.keys(data),
      });
      const { userId, ...body } = data;
      return put<User>("/api/admin/user/update", body, { userId });
    },

    // 删除用户
    deleteUser: (userId: number) => {
      apiLog.requestStart("DELETE", "/api/admin/user/delete", { userId });
      return del<{ success: boolean }>("/api/admin/user/delete", { userId });
    },

    // 获取文件列表
    getFileList: (params?: AdminFileListParams) => {
      apiLog.requestStart("GET", "/api/admin/file/list", params);
      return get<PaginatedResponse<any>>("/api/admin/file/list", params);
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