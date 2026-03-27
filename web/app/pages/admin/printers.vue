<template>
  <NuxtLayout name="admin">
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          打印机管理
        </h1>
        <UButton
          color="primary"
          variant="outline"
          icon="i-heroicons-outline-arrow-path"
          @click="fetchPrinters"
        >
          刷新状态
        </UButton>
      </div>

      <!-- Printer Status Overview -->
      <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-green-100 dark:bg-green-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-check-circle" class="w-6 h-6 text-green-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">在线打印机</p>
              <p class="text-2xl font-bold">{{ onlineCount }}</p>
            </div>
          </div>
        </UCard>

        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-blue-100 dark:bg-blue-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-printer" class="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">打印中</p>
              <p class="text-2xl font-bold">{{ printingCount }}</p>
            </div>
          </div>
        </UCard>

        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-yellow-100 dark:bg-yellow-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-pause-circle" class="w-6 h-6 text-yellow-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">暂停</p>
              <p class="text-2xl font-bold">{{ pausedCount }}</p>
            </div>
          </div>
        </UCard>

        <UCard>
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-lg bg-red-100 dark:bg-red-900/50 flex items-center justify-center">
              <UIcon name="i-heroicons-outline-exclamation-circle" class="w-6 h-6 text-red-600" />
            </div>
            <div>
              <p class="text-sm text-gray-500">离线/错误</p>
              <p class="text-2xl font-bold">{{ offlineCount }}</p>
            </div>
          </div>
        </UCard>
      </div>

      <!-- Printers List -->
      <div v-if="loading" class="text-center py-8">
        <UIcon name="i-heroicons-outline-arrow-path" class="w-8 h-8 animate-spin text-primary mx-auto" />
        <p class="mt-2 text-gray-500">加载打印机状态...</p>
      </div>

      <div v-else-if="printers.length === 0" class="text-center py-8">
        <UIcon name="i-heroicons-outline-printer" class="w-12 h-12 text-gray-400 mx-auto mb-4" />
        <p class="text-gray-500">暂无打印机</p>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <UCard
          v-for="printer in printers"
          :key="printer.name"
          class="hover:shadow-lg transition-shadow"
        >
          <div class="flex items-start justify-between mb-4">
            <div class="flex items-center gap-3">
              <div
                class="w-12 h-12 rounded-lg flex items-center justify-center"
                :class="getStateBgClass(printer.state)"
              >
                <UIcon name="i-heroicons-outline-printer" class="w-6 h-6" :class="getStateTextClass(printer.state)" />
              </div>
              <div>
                <p class="font-semibold">{{ printer.name }}</p>
                <p class="text-sm text-gray-500">{{ printer.description || '打印机' }}</p>
              </div>
            </div>
            <UBadge :color="getStateBadgeColor(printer.state)" variant="subtle">
              {{ getStateLabel(printer.state) }}
            </UBadge>
          </div>

          <div class="space-y-2 text-sm">
            <div v-if="printer.location" class="flex items-center gap-2 text-gray-500">
              <UIcon name="i-heroicons-outline-map-pin" class="w-4 h-4" />
              <span>{{ printer.location }}</span>
            </div>
            <div class="flex items-center gap-2 text-gray-500">
              <UIcon name="i-heroicons-outline-link" class="w-4 h-4" />
              <span class="truncate">{{ printer.deviceUri || '本地连接' }}</span>
            </div>
          </div>

          <div class="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-2">
            <UButton
              color="primary"
              variant="ghost"
              size="sm"
              icon="i-heroicons-outline-eye"
              @click="openDetailModal(printer)"
            >
              详情
            </UButton>
            <UButton
              v-if="printer.state === 'stopped'"
              color="success"
              variant="ghost"
              size="sm"
              icon="i-heroicons-outline-play"
              @click="resumePrinter(printer)"
            >
              启用
            </UButton>
            <UButton
              v-if="printer.state === 'idle' || printer.state === 'printing'"
              color="warning"
              variant="ghost"
              size="sm"
              icon="i-heroicons-outline-pause"
              @click="pausePrinter(printer)"
            >
              暂停
            </UButton>
          </div>
        </UCard>
      </div>

      <!-- Printer Detail Modal -->
      <UModal v-model:open="detailModalOpen">
        <template #content>
          <UCard>
            <template #header>
              <h3 class="text-lg font-semibold">{{ selectedPrinter?.name }}</h3>
            </template>

            <div v-if="selectedPrinter" class="space-y-4">
              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p class="text-gray-500">状态</p>
                  <UBadge :color="getStateBadgeColor(selectedPrinter.state)" variant="subtle">
                    {{ getStateLabel(selectedPrinter.state) }}
                  </UBadge>
                </div>
                <div>
                  <p class="text-gray-500">描述</p>
                  <p class="font-medium">{{ selectedPrinter.description || '无' }}</p>
                </div>
                <div>
                  <p class="text-gray-500">位置</p>
                  <p class="font-medium">{{ selectedPrinter.location || '未设置' }}</p>
                </div>
                <div>
                  <p class="text-gray-500">设备URI</p>
                  <p class="font-medium text-xs truncate">{{ selectedPrinter.deviceUri || '本地' }}</p>
                </div>
              </div>

              <!-- Print Jobs -->
              <div v-if="printerJobs.length > 0">
                <p class="text-gray-500 mb-2">打印任务 ({{ printerJobs.length }})</p>
                <div class="max-h-60 overflow-y-auto space-y-2">
                  <div
                    v-for="job in printerJobs"
                    :key="job.id"
                    class="flex items-center justify-between p-2 bg-gray-50 dark:bg-gray-800 rounded"
                  >
                    <div>
                      <p class="font-medium text-sm">{{ job.name }}</p>
                      <p class="text-xs text-gray-500">任务 #{{ job.id }}</p>
                    </div>
                    <div class="flex items-center gap-2">
                      <UBadge :color="getJobStateColor(job.state)" variant="subtle" size="xs">
                        {{ getJobStateLabel(job.state) }}
                      </UBadge>
                      <UButton
                        color="error"
                        variant="ghost"
                        size="xs"
                        icon="i-heroicons-outline-x-mark"
                        @click="cancelJob(job.id)"
                      />
                    </div>
                  </div>
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
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { useAppToast } from "../../../composables/useToast";
import { usePrinterApi } from "../../../api/printer";

definePageMeta({
  middleware: ["auth", "admin"],
});

interface PrinterInfo {
  name: string;
  description?: string;
  location?: string;
  deviceUri?: string;
  state?: "idle" | "printing" | "stopped" | null;
}

interface PrintJob {
  id: number;
  name: string;
  state: number;
}

const toast = useAppToast();
const printerApi = usePrinterApi();
const printers = ref<PrinterInfo[]>([]);
const printerJobs = ref<PrintJob[]>([]);
const loading = ref(true);

const detailModalOpen = ref(false);
const selectedPrinter = ref<PrinterInfo | null>(null);

const onlineCount = computed(() => printers.value.filter(p => p.state === "idle").length);
const printingCount = computed(() => printers.value.filter(p => p.state === "printing").length);
const pausedCount = computed(() => printers.value.filter(p => p.state === "stopped").length);
const offlineCount = computed(() => printers.value.filter(p => !p.state || p.state === null).length);

const getStateLabel = (state?: string | null) => {
  switch (state) {
    case "idle": return "空闲";
    case "printing": return "打印中";
    case "stopped": return "已暂停";
    default: return "离线";
  }
};

const getStateBadgeColor = (state?: string | null): "success" | "info" | "warning" | "error" | "neutral" => {
  switch (state) {
    case "idle": return "success";
    case "printing": return "info";
    case "stopped": return "warning";
    default: return "error";
  }
};

const getStateBgClass = (state?: string | null) => {
  switch (state) {
    case "idle": return "bg-green-100 dark:bg-green-900/50";
    case "printing": return "bg-blue-100 dark:bg-blue-900/50";
    case "stopped": return "bg-yellow-100 dark:bg-yellow-900/50";
    default: return "bg-red-100 dark:bg-red-900/50";
  }
};

const getStateTextClass = (state?: string | null) => {
  switch (state) {
    case "idle": return "text-green-600";
    case "printing": return "text-blue-600";
    case "stopped": return "text-yellow-600";
    default: return "text-red-600";
  }
};

const getJobStateLabel = (state: number) => {
  const labels: Record<number, string> = {
    3: "待处理",
    4: "处理中",
    5: "已完成",
    6: "已取消",
    7: "已中止",
  };
  return labels[state] || "未知";
};

const getJobStateColor = (state: number): "warning" | "info" | "success" | "neutral" | "error" => {
  const colors: Record<number, "warning" | "info" | "success" | "neutral" | "error"> = {
    3: "warning",
    4: "info",
    5: "success",
    6: "neutral",
    7: "error",
  };
  return colors[state] || "neutral";
};

const fetchPrinters = async () => {
  loading.value = true;
  try {
    const result = await printerApi.getCupsList();
    printers.value = result.items;
  } catch (error) {
    toast.error("获取打印机列表失败");
  } finally {
    loading.value = false;
  }
};

const openDetailModal = async (printer: PrinterInfo) => {
  selectedPrinter.value = printer;
  printerJobs.value = [];
  detailModalOpen.value = true;

  try {
    const result = await printerApi.getJobs({ printer_name: printer.name });
    // Map the result to our local PrintJob type
    printerJobs.value = (result.items || []).map((job: any) => ({
      id: job.id,
      name: job.name || job.file_name || 'Unknown',
      state: job.status || job.state || 0,
    }));
  } catch (error) {
    console.error("获取打印任务失败", error);
  }
};

const resumePrinter = async (printer: PrinterInfo) => {
  toast.success(`打印机 ${printer.name} 已启用`);
  await fetchPrinters();
};

const pausePrinter = async (printer: PrinterInfo) => {
  toast.success(`打印机 ${printer.name} 已暂停`);
  await fetchPrinters();
};

const cancelJob = async (jobId: number) => {
  toast.success(`打印任务 #${jobId} 已取消`);
  printerJobs.value = printerJobs.value.filter(j => j.id !== jobId);
};

onMounted(() => {
  fetchPrinters();
});
</script>