<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        订单管理
      </h1>
      <USelect
        v-model="statusFilter"
        :items="statusOptions"
        placeholder="筛选状态"
        class="w-40"
        @update:model-value="fetchOrders"
      />
    </div>

    <!-- Orders Table -->
    <UCard>
      <UTable :data="orders" :columns="columns">
        <template #status-cell="{ row }">
          <UBadge :color="statusColors[row.original.status]" variant="subtle">
            {{ statusLabels[row.original.status] }}
          </UBadge>
        </template>

        <template #finalAmount-cell="{ row }">
          ¥{{ row.original.finalAmount.toFixed(2) }}
        </template>

        <template #createdAt-cell="{ row }">
          {{ formatDate(row.original.createdAt) }}
        </template>
      </UTable>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <Pagination
          v-model:current-page="currentPage"
          :total-pages="totalPages"
          @change="fetchOrders"
        />
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import type { Order } from "../../../types/order";
import { useAppToast } from "../../../composables/useToast";
import { useAdminApi } from "../../../api/admin";

definePageMeta({
  middleware: ["auth", "admin"],
});

const toast = useAppToast();
const adminApi = useAdminApi();
const orders = ref<Order[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 10;
const statusFilter = ref<string>("");

const statusOptions = [
  { value: "", label: "全部状态" },
  { value: "0", label: "待处理" },
  { value: "1", label: "打印中" },
  { value: "2", label: "已完成" },
  { value: "3", label: "已取消" },
  { value: "4", label: "失败" },
];

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

const columns = [
  { accessorKey: "id", header: "订单ID" },
  { accessorKey: "userId", header: "用户ID" },
  { accessorKey: "fileName", header: "文件名" },
  { accessorKey: "printerName", header: "打印机" },
  { accessorKey: "copies", header: "份数" },
  { accessorKey: "status", header: "状态" },
  { accessorKey: "finalAmount", header: "价格" },
  { accessorKey: "createdAt", header: "创建时间" },
];

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const fetchOrders = async () => {
  try {
    const result = await adminApi.getOrderList({
      page: currentPage.value,
      pageSize: pageSize,
      status: statusFilter.value ? Number(statusFilter.value) as Order["status"] : undefined,
    });
    orders.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
  } catch (error) {
    toast.error("获取订单列表失败");
  }
};

onMounted(() => {
  fetchOrders();
});
</script>