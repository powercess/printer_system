<template>
  <NuxtLayout name="admin">
    <div class="space-y-6">
      <!-- Stats Cards -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-users" class="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">总用户数</p>
              <p class="text-2xl font-bold">{{ stats?.total_users || 0 }}</p>
            </div>
          </div>
        </UCard>

        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-document-text" class="w-6 h-6 text-green-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">总订单数</p>
              <p class="text-2xl font-bold">{{ stats?.total_orders || 0 }}</p>
            </div>
          </div>
        </UCard>

        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-purple-100 dark:bg-purple-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-currency-yen" class="w-6 h-6 text-purple-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">总收入</p>
              <p class="text-2xl font-bold">¥{{ stats?.total_revenue?.toFixed(2) || "0.00" }}</p>
            </div>
          </div>
        </UCard>

        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-orange-100 dark:bg-orange-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-printer" class="w-6 h-6 text-orange-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">活跃打印机</p>
              <p class="text-2xl font-bold">{{ stats?.active_printers || 0 }}</p>
            </div>
          </div>
        </UCard>
      </div>

      <!-- Recent Activity -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- Recent Orders -->
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="font-semibold">最近订单</h3>
              <NuxtLink to="/admin/orders" class="text-sm text-primary hover:underline">
                查看全部
              </NuxtLink>
            </div>
          </template>
          <div v-if="loadingOrders" class="py-8 text-center text-gray-500">
            加载中...
          </div>
          <div v-else-if="recentOrders.length === 0" class="py-8 text-center text-gray-500">
            暂无订单
          </div>
          <div v-else class="divide-y divide-gray-200 dark:divide-gray-700">
            <div
              v-for="order in recentOrders"
              :key="order.id"
              class="flex items-center justify-between py-3"
            >
              <div>
                <p class="font-medium">订单 #{{ order.id }}</p>
                <p class="text-sm text-gray-500">{{ order.fileName || '未知文件' }}</p>
              </div>
              <div class="text-right">
                <p class="font-medium">¥{{ order.finalAmount?.toFixed(2) }}</p>
                <UBadge :color="statusColors[order.status]" variant="subtle" size="xs">
                  {{ statusLabels[order.status] }}
                </UBadge>
              </div>
            </div>
          </div>
        </UCard>

        <!-- Recent Users -->
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="font-semibold">最近用户</h3>
              <NuxtLink to="/admin/users" class="text-sm text-primary hover:underline">
                查看全部
              </NuxtLink>
            </div>
          </template>
          <div v-if="loadingUsers" class="py-8 text-center text-gray-500">
            加载中...
          </div>
          <div v-else-if="recentUsers.length === 0" class="py-8 text-center text-gray-500">
            暂无用户
          </div>
          <div v-else class="divide-y divide-gray-200 dark:divide-gray-700">
            <div
              v-for="user in recentUsers"
              :key="user.id"
              class="flex items-center justify-between py-3"
            >
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                  <span class="font-medium">{{ (user.nickname || user.username).charAt(0).toUpperCase() }}</span>
                </div>
                <div>
                  <p class="font-medium">{{ user.nickname || user.username }}</p>
                  <p class="text-sm text-gray-500">{{ user.email }}</p>
                </div>
              </div>
              <UBadge :color="user.groupId === 0 ? 'primary' : 'neutral'" variant="subtle" size="xs">
                {{ user.groupId === 0 ? '管理员' : '用户' }}
              </UBadge>
            </div>
          </div>
        </UCard>
      </div>

      <!-- Quick Actions -->
      <UCard>
        <template #header>
          <h3 class="font-semibold">快捷操作</h3>
        </template>
        <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <UButton
            color="primary"
            variant="outline"
            icon="i-heroicons-outline-user-plus"
            to="/admin/users"
            block
          >
            添加用户
          </UButton>
          <UButton
            color="primary"
            variant="outline"
            icon="i-heroicons-outline-clipboard-document-list"
            to="/admin/orders"
            block
          >
            查看订单
          </UButton>
          <UButton
            color="primary"
            variant="outline"
            icon="i-heroicons-outline-printer"
            to="/admin/printers"
            block
          >
            打印机状态
          </UButton>
          <UButton
            color="primary"
            variant="outline"
            icon="i-heroicons-outline-cog-6-tooth"
            to="/admin/settings"
            block
          >
            系统设置
          </UButton>
        </div>
      </UCard>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import type { User } from "../../../types/user";
import type { Order } from "../../../types/order";
import { useAppToast } from "../../../composables/useToast";
import { useAdminApi } from "../../../api/admin";

definePageMeta({
  middleware: ["auth", "admin"],
});

const toast = useAppToast();
const adminApi = useAdminApi();

interface AdminStats {
  total_users: number;
  total_orders: number;
  total_revenue: number;
  today_orders: number;
  today_revenue: number;
  active_printers: number;
}

const stats = ref<AdminStats | null>(null);
const recentOrders = ref<Order[]>([]);
const recentUsers = ref<User[]>([]);
const loadingOrders = ref(true);
const loadingUsers = ref(true);

const statusLabels: Record<number, string> = {
  0: "待处理",
  1: "打印中",
  2: "已完成",
  3: "已取消",
  4: "失败",
};

const statusColors: Record<number, "warning" | "info" | "success" | "neutral" | "error"> = {
  0: "warning",
  1: "info",
  2: "success",
  3: "neutral",
  4: "error",
};

const fetchStats = async () => {
  try {
    stats.value = await adminApi.getStats();
  } catch (error) {
    toast.error("获取统计数据失败");
  }
};

const fetchRecentOrders = async () => {
  loadingOrders.value = true;
  try {
    const result = await adminApi.getOrderList({ page: 1, pageSize: 5 });
    recentOrders.value = result.items;
  } catch (error) {
    toast.error("获取订单失败");
  } finally {
    loadingOrders.value = false;
  }
};

const fetchRecentUsers = async () => {
  loadingUsers.value = true;
  try {
    const result = await adminApi.getUserList({ page: 1, pageSize: 5 });
    recentUsers.value = result.items;
  } catch (error) {
    toast.error("获取用户失败");
  } finally {
    loadingUsers.value = false;
  }
};

onMounted(() => {
  fetchStats();
  fetchRecentOrders();
  fetchRecentUsers();
});
</script>