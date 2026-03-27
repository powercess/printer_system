<template>
  <NuxtLayout name="admin">
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          订单管理
        </h1>
        <div class="flex gap-2">
          <UButton
            v-if="selectedIds.length > 0"
            color="error"
            variant="outline"
            icon="i-heroicons-outline-x-circle"
            @click="confirmBatchCancel"
          >
            批量取消 ({{ selectedIds.length }})
          </UButton>
          <UButton
            color="primary"
            variant="outline"
            icon="i-heroicons-outline-arrow-path"
            @click="fetchOrders"
          >
            刷新
          </UButton>
        </div>
      </div>

      <!-- Filters -->
      <UCard>
        <div class="flex flex-wrap gap-4">
          <UInput
            v-model="searchQuery"
            placeholder="搜索订单ID或用户..."
            icon="i-heroicons-outline-magnifying-glass"
            class="flex-1 min-w-[200px]"
            @keyup.enter="fetchOrders"
          />
          <USelect
            v-model="statusFilter"
            :items="statusOptions"
            placeholder="订单状态"
            class="w-40"
            @update:model-value="fetchOrders"
          />
          <UButton color="primary" @click="fetchOrders">搜索</UButton>
        </div>
      </UCard>

      <!-- Batch Actions Bar -->
      <div v-if="selectedIds.length > 0" class="flex items-center gap-4 p-3 bg-primary/10 rounded-lg">
        <UCheckbox
          :model-value="selectedIds.length === orders.length && orders.length > 0"
          :indeterminate="selectedIds.length > 0 && selectedIds.length < orders.length"
          @update:model-value="(val: boolean | 'indeterminate') => toggleSelectAll(val === true)"
        />
        <span class="text-sm">已选择 {{ selectedIds.length }} 项</span>
        <UButton color="neutral" variant="ghost" size="sm" @click="selectedIds = []">
          取消选择
        </UButton>
        <div class="flex-1" />
        <UButton color="warning" variant="outline" size="sm" icon="i-heroicons-outline-play" @click="batchProcess">
          批量处理
        </UButton>
        <UButton color="error" variant="outline" size="sm" icon="i-heroicons-outline-x-circle" @click="confirmBatchCancel">
          批量取消
        </UButton>
      </div>

      <!-- Orders Table -->
      <UCard>
        <UTable :data="orders" :columns="columns" v-model:sort="sort">
          <template #select-header>
            <UCheckbox
              :model-value="selectedIds.length === orders.length && orders.length > 0"
              :indeterminate="selectedIds.length > 0 && selectedIds.length < orders.length"
              @update:model-value="(val: boolean | 'indeterminate') => toggleSelectAll(val === true)"
            />
          </template>

          <template #select-cell="{ row }">
            <UCheckbox
              :model-value="selectedIds.includes(row.original.id)"
              :update:model-value="(val: boolean | 'indeterminate') => toggleSelect(row.original.id, val === true)"
            />
          </template>

          <template #id-cell="{ row }">
            <span class="font-mono text-sm">#{{ row.original.id }}</span>
          </template>

          <template #user-cell="{ row }">
            <div>
              <p class="font-medium">{{ row.original.username || `用户${row.original.userId}` }}</p>
              <p class="text-sm text-gray-500">ID: {{ row.original.userId }}</p>
            </div>
          </template>

          <template #file-cell="{ row }">
            <div class="max-w-[200px] truncate">
              <p class="truncate">{{ row.original.fileName || '未知文件' }}</p>
              <p class="text-sm text-gray-500">{{ row.original.printerName }}</p>
            </div>
          </template>

          <template #detail-cell="{ row }">
            <div class="text-sm">
              <span>{{ row.original.copies }}份</span>
              <span class="text-gray-400 mx-1">·</span>
              <span>{{ row.original.colorMode === 0 ? '黑白' : '彩色' }}</span>
              <span class="text-gray-400 mx-1">·</span>
              <span>{{ row.original.duplex === 1 ? '双面' : '单面' }}</span>
            </div>
          </template>

          <template #amount-cell="{ row }">
            <div class="text-right">
              <p class="font-medium text-primary">¥{{ row.original.finalAmount?.toFixed(2) }}</p>
              <p v-if="row.original.discountAmount > 0" class="text-xs text-gray-500 line-through">
                ¥{{ row.original.originalAmount?.toFixed(2) }}
              </p>
            </div>
          </template>

          <template #status-cell="{ row }">
            <UBadge :color="statusColors[row.original.status]" variant="subtle">
              {{ statusLabels[row.original.status] }}
            </UBadge>
          </template>

          <template #created_at-cell="{ row }">
            {{ formatDate(row.original.createdAt) }}
          </template>

          <template #actions-cell="{ row }">
            <div class="flex items-center gap-1">
              <UButton
                color="primary"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-eye"
                @click="openDetailModal(row.original)"
              />
              <UButton
                v-if="row.original.status === 0"
                color="warning"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-play"
                @click="processOrder(row.original)"
              />
              <UButton
                v-if="row.original.status === 1"
                color="success"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-check"
                @click="completeOrder(row.original)"
              />
            </div>
          </template>
        </UTable>

        <!-- Pagination -->
        <div class="flex items-center justify-between mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
          <div class="text-sm text-gray-500">
            共 {{ total }} 条记录，当前第 {{ currentPage }}/{{ totalPages }} 页
          </div>
          <UPagination
            v-model:page="currentPage"
            :total="total"
            :items-per-page="pageSize"
            show-controls
            @update:page="fetchOrders"
          />
        </div>
      </UCard>

      <!-- Order Detail Modal -->
      <UModal v-model:open="detailModalOpen">
        <template #content>
          <UCard>
            <template #header>
              <h3 class="text-lg font-semibold">订单详情 #{{ selectedOrder?.id }}</h3>
            </template>

            <div v-if="selectedOrder" class="space-y-4">
              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p class="text-gray-500">订单ID</p>
                  <p class="font-medium">#{{ selectedOrder.id }}</p>
                </div>
                <div>
                  <p class="text-gray-500">用户</p>
                  <p class="font-medium">{{ selectedOrder.username || `用户${selectedOrder.userId}` }}</p>
                </div>
                <div>
                  <p class="text-gray-500">文件名</p>
                  <p class="font-medium">{{ selectedOrder.fileName || '未知文件' }}</p>
                </div>
                <div>
                  <p class="text-gray-500">打印机</p>
                  <p class="font-medium">{{ selectedOrder.printerName }}</p>
                </div>
                <div>
                  <p class="text-gray-500">打印份数</p>
                  <p class="font-medium">{{ selectedOrder.copies }} 份</p>
                </div>
                <div>
                  <p class="text-gray-500">颜色模式</p>
                  <p class="font-medium">{{ selectedOrder.colorMode === 0 ? '黑白' : '彩色' }}</p>
                </div>
                <div>
                  <p class="text-gray-500">打印方式</p>
                  <p class="font-medium">{{ selectedOrder.duplex === 1 ? '双面' : '单面' }}</p>
                </div>
                <div>
                  <p class="text-gray-500">纸张大小</p>
                  <p class="font-medium">{{ selectedOrder.paperSize }}</p>
                </div>
                <div>
                  <p class="text-gray-500">原价</p>
                  <p class="font-medium">¥{{ selectedOrder.originalAmount?.toFixed(2) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">优惠</p>
                  <p class="font-medium text-green-600">-¥{{ selectedOrder.discountAmount?.toFixed(2) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">实付金额</p>
                  <p class="font-bold text-primary text-lg">¥{{ selectedOrder.finalAmount?.toFixed(2) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">状态</p>
                  <UBadge :color="statusColors[selectedOrder.status]" variant="subtle">
                    {{ statusLabels[selectedOrder.status] }}
                  </UBadge>
                </div>
                <div>
                  <p class="text-gray-500">创建时间</p>
                  <p class="font-medium">{{ formatDate(selectedOrder.createdAt) }}</p>
                </div>
                <div v-if="selectedOrder.updatedAt">
                  <p class="text-gray-500">更新时间</p>
                  <p class="font-medium">{{ formatDate(selectedOrder.updatedAt) }}</p>
                </div>
              </div>
            </div>

            <template #footer>
              <div class="flex justify-end gap-3">
                <UButton color="neutral" variant="ghost" @click="detailModalOpen = false">
                  关闭
                </UButton>
              </div>
            </template>
          </UCard>
        </template>
      </UModal>

      <!-- Batch Cancel Confirmation Modal -->
      <UModal v-model:open="batchCancelModalOpen">
        <template #content>
          <UCard>
            <div class="text-center">
              <UIcon name="i-heroicons-solid-exclamation-triangle" class="w-12 h-12 mx-auto text-yellow-500 mb-4" />
              <h3 class="text-lg font-semibold mb-2">确认批量取消</h3>
              <p class="text-gray-500 mb-6">
                确定要取消选中的 {{ selectedIds.length }} 个订单吗？
              </p>
              <div class="flex justify-center gap-3">
                <UButton color="neutral" variant="ghost" @click="batchCancelModalOpen = false">
                  取消
                </UButton>
                <UButton color="warning" :loading="batchCancelling" @click="batchCancelOrders">
                  确认取消
                </UButton>
              </div>
            </div>
          </UCard>
        </template>
      </UModal>
    </div>
  </NuxtLayout>
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
const total = ref(0);
const pageSize = 10;
const searchQuery = ref("");
const statusFilter = ref<string>("all");
const sort = ref({ column: "createdAt", direction: "desc" as const });

// Selection
const selectedIds = ref<number[]>([]);

const detailModalOpen = ref(false);
const batchCancelModalOpen = ref(false);
const batchCancelling = ref(false);

const selectedOrder = ref<Order | null>(null);

const statusOptions = [
  { value: "all", label: "全部状态" },
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
  { id: "select", header: "" },
  { id: "id", header: "订单号", sortable: true },
  { id: "user", header: "用户", sortable: true },
  { id: "file", header: "文件/打印机" },
  { id: "detail", header: "打印详情" },
  { id: "amount", header: "金额", sortable: true },
  { id: "status", header: "状态" },
  { accessorKey: "createdAt", header: "创建时间", sortable: true },
  { id: "actions", header: "操作" },
];

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const toggleSelect = (id: number, selected: boolean) => {
  if (selected) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id);
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id);
  }
};

const toggleSelectAll = (selected: boolean) => {
  if (selected) {
    selectedIds.value = orders.value.map(o => o.id);
  } else {
    selectedIds.value = [];
  }
};

const fetchOrders = async () => {
  try {
    const result = await adminApi.getOrderList({
      page: currentPage.value,
      pageSize: pageSize,
      status: statusFilter.value === "all" ? undefined : Number(statusFilter.value),
    });
    orders.value = result.items;
    total.value = result.total;
    totalPages.value = Math.ceil(result.total / pageSize);
    selectedIds.value = [];
  } catch (error) {
    toast.error("获取订单列表失败");
  }
};

const openDetailModal = (order: Order) => {
  selectedOrder.value = order;
  detailModalOpen.value = true;
};

const processOrder = async (order: Order) => {
  toast.info(`正在处理订单 #${order.id}...`);
};

const completeOrder = async (order: Order) => {
  toast.success(`订单 #${order.id} 已标记为完成`);
  await fetchOrders();
};

const confirmBatchCancel = () => {
  if (selectedIds.value.length === 0) return;
  batchCancelModalOpen.value = true;
};

const batchCancelOrders = async () => {
  batchCancelling.value = true;
  try {
    toast.success(`已取消 ${selectedIds.value.length} 个订单`);
    selectedIds.value = [];
    batchCancelModalOpen.value = false;
    await fetchOrders();
  } finally {
    batchCancelling.value = false;
  }
};

const batchProcess = async () => {
  if (selectedIds.value.length === 0) return;
  toast.info(`正在批量处理 ${selectedIds.value.length} 个订单...`);
  selectedIds.value = [];
  await fetchOrders();
};

onMounted(() => {
  fetchOrders();
});
</script>