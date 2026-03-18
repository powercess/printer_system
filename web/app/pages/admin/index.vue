<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        管理后台
      </h1>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <UCard>
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/50 flex items-center justify-center">
            <UIcon name="i-heroicons-users" class="w-6 h-6 text-blue-600" />
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
            <UIcon name="i-heroicons-document-text" class="w-6 h-6 text-green-600" />
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
            <UIcon name="i-heroicons-currency-yen" class="w-6 h-6 text-purple-600" />
          </div>
          <div>
            <p class="text-sm text-gray-500">总收入</p>
            <p class="text-2xl font-bold">¥{{ stats?.total_revenue?.toFixed(2) || "0.00" }}</p>
          </div>
        </div>
      </UCard>

      <UCard>
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-lg bg-yellow-100 dark:bg-yellow-900/50 flex items-center justify-center">
            <UIcon name="i-heroicons-calendar" class="w-6 h-6 text-yellow-600" />
          </div>
          <div>
            <p class="text-sm text-gray-500">今日订单</p>
            <p class="text-2xl font-bold">{{ stats?.today_orders || 0 }}</p>
          </div>
        </div>
      </UCard>
    </div>

    <!-- Quick Links -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <NuxtLink to="/admin/users">
        <UCard class="hover:border-primary transition-colors cursor-pointer">
          <div class="flex items-center gap-3">
            <UIcon name="i-heroicons-users" class="w-8 h-8 text-primary" />
            <div>
              <p class="font-semibold">用户管理</p>
              <p class="text-sm text-gray-500">管理系统用户</p>
            </div>
          </div>
        </UCard>
      </NuxtLink>

      <NuxtLink to="/admin/files">
        <UCard class="hover:border-primary transition-colors cursor-pointer">
          <div class="flex items-center gap-3">
            <UIcon name="i-heroicons-document" class="w-8 h-8 text-primary" />
            <div>
              <p class="font-semibold">文件管理</p>
              <p class="text-sm text-gray-500">查看所有文件</p>
            </div>
          </div>
        </UCard>
      </NuxtLink>

      <NuxtLink to="/admin/orders">
        <UCard class="hover:border-primary transition-colors cursor-pointer">
          <div class="flex items-center gap-3">
            <UIcon name="i-heroicons-clipboard-document-list" class="w-8 h-8 text-primary" />
            <div>
              <p class="font-semibold">订单管理</p>
              <p class="text-sm text-gray-500">查看所有订单</p>
            </div>
          </div>
        </UCard>
      </NuxtLink>

      <NuxtLink to="/admin/logs">
        <UCard class="hover:border-primary transition-colors cursor-pointer">
          <div class="flex items-center gap-3">
            <UIcon name="i-heroicons-document-text" class="w-8 h-8 text-primary" />
            <div>
              <p class="font-semibold">日志管理</p>
              <p class="text-sm text-gray-500">查看系统日志</p>
            </div>
          </div>
        </UCard>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
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
}

const stats = ref<AdminStats | null>(null);

const fetchStats = async () => {
  try {
    stats.value = await adminApi.getStats();
  } catch (error) {
    toast.error("获取统计数据失败");
  }
};

onMounted(() => {
  fetchStats();
});
</script>