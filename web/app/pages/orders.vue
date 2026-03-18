<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        订单记录
      </h1>

      <USelect
        v-model="statusFilter"
        :items="statusOptions"
        placeholder="筛选状态"
        class="w-40"
        @update:model-value="fetchOrders"
      />
    </div>

    <!-- Orders List -->
    <div v-if="loading" class="flex justify-center py-8">
      <LoadingSpinner />
    </div>

    <div v-else-if="orders.length === 0" class="text-center py-8 text-gray-500">
      <UIcon name="i-heroicons-document-text" class="w-12 h-12 mx-auto mb-4 opacity-50" />
      <p>暂无订单记录</p>
    </div>

    <div v-else class="space-y-4">
      <UCard v-for="order in orders" :key="order.id">
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div class="flex items-start gap-3">
            <UIcon name="i-heroicons-document-text" class="w-10 h-10 text-primary flex-shrink-0" />
            <div>
              <p class="font-medium">{{ order.file_name }}</p>
              <div class="flex flex-wrap items-center gap-2 text-sm text-gray-500 mt-1">
                <span>{{ order.printer_name }}</span>
                <span>·</span>
                <span>{{ order.copies }} 份</span>
                <span>·</span>
                <span>{{ order.color_mode === "bw" ? "黑白" : "彩色" }}</span>
                <span>·</span>
                <span>{{ order.paper_size }}</span>
              </div>
              <p class="text-sm text-gray-500 mt-1">
                {{ formatDate(order.created_at) }}
              </p>
            </div>
          </div>

          <div class="flex items-center gap-4">
            <div class="text-right">
              <p class="text-lg font-bold text-primary">
                ¥{{ order.price.toFixed(2) }}
              </p>
              <UBadge
                :color="statusColors[order.status] as 'primary' | 'secondary' | 'success' | 'info' | 'warning' | 'error' | 'neutral'"
                variant="subtle"
                size="sm"
              >
                {{ statusLabels[order.status] }}
              </UBadge>
            </div>

            <UButton
              v-if="order.status === 'pending'"
              color="error"
              variant="ghost"
              size="sm"
              @click="confirmCancel(order)"
            >
              取消
            </UButton>
          </div>
        </div>
      </UCard>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center">
      <Pagination
        v-model:current-page="currentPage"
        :total-pages="totalPages"
        @change="fetchOrders"
      />
    </div>

    <!-- Cancel Confirmation Modal -->
    <UModal v-model:open="cancelModalOpen">
      <template #content>
        <UCard>
          <div class="text-center">
            <UIcon name="i-heroicons-exclamation-triangle" class="w-12 h-12 mx-auto text-yellow-500 mb-4" />
            <h3 class="text-lg font-semibold mb-2">确认取消订单</h3>
            <p class="text-gray-500 mb-6">
              确定要取消此订单吗？取消后将退还费用到账户余额。
            </p>
            <div class="flex justify-center gap-3">
              <UButton color="neutral" variant="ghost" @click="cancelModalOpen = false">
                返回
              </UButton>
              <UButton color="error" :loading="cancelling" @click="cancelOrder">
                确认取消
              </UButton>
            </div>
          </div>
        </UCard>
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import type { Order } from "../../types/order";
import { useAppToast } from "../../composables/useToast";
import { useOrderApi } from "../../api/order";

definePageMeta({
  middleware: ["auth"],
});

const toast = useAppToast();
const loading = ref(true);
const orders = ref<Order[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 10;
const statusFilter = ref<string>("");

const cancelModalOpen = ref(false);
const orderToCancel = ref<Order | null>(null);
const cancelling = ref(false);

const orderApi = useOrderApi();

const statusOptions = [
  { value: "", label: "全部状态" },
  { value: "pending", label: "待处理" },
  { value: "printing", label: "打印中" },
  { value: "completed", label: "已完成" },
  { value: "cancelled", label: "已取消" },
  { value: "failed", label: "失败" },
];

const statusLabels: Record<string, string> = {
  pending: "待处理",
  printing: "打印中",
  completed: "已完成",
  cancelled: "已取消",
  failed: "失败",
};

const statusColors: Record<string, "warning" | "info" | "success" | "neutral" | "error"> = {
  pending: "warning",
  printing: "info",
  completed: "success",
  cancelled: "neutral",
  failed: "error",
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const result = await orderApi.getList({
      page: currentPage.value,
      page_size: pageSize,
      status: statusFilter.value as Order["status"] || undefined,
    });
    orders.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
  } catch (error) {
    toast.error("获取订单列表失败");
  } finally {
    loading.value = false;
  }
};

const confirmCancel = (order: Order) => {
  orderToCancel.value = order;
  cancelModalOpen.value = true;
};

const cancelOrder = async () => {
  if (!orderToCancel.value) return;

  cancelling.value = true;
  try {
    await orderApi.cancel(orderToCancel.value.id);
    toast.success("订单已取消");
    await fetchOrders();
  } catch (error) {
    toast.error("取消订单失败");
  } finally {
    cancelling.value = false;
    cancelModalOpen.value = false;
    orderToCancel.value = null;
  }
};

onMounted(() => {
  fetchOrders();
});
</script>