<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        日志管理
      </h1>
    </div>

    <!-- Logs Table -->
    <UCard>
      <div v-if="loading" class="flex justify-center py-8">
        <LoadingSpinner />
      </div>

      <div v-else-if="logs.length === 0" class="text-center py-8 text-gray-500">
        暂无日志记录
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="log in logs"
          :key="log.id"
          class="p-4 rounded-lg border border-gray-200 dark:border-gray-700"
        >
          <div class="flex items-start justify-between">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <UBadge :color="logColors[log.level]" variant="subtle" size="sm">
                  {{ log.level }}
                </UBadge>
                <span class="text-sm text-gray-500">{{ formatDate(log.created_at) }}</span>
              </div>
              <p class="text-gray-700 dark:text-gray-300">{{ log.message }}</p>
              <p v-if="log.details" class="text-sm text-gray-500 mt-1">
                {{ log.details }}
              </p>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <Pagination
          v-model:current-page="currentPage"
          :total-pages="totalPages"
          @change="fetchLogs"
        />
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import { useAppToast } from "../../../composables/useToast";

definePageMeta({
  middleware: ["auth", "admin"],
});

interface Log {
  id: string;
  level: "info" | "warning" | "error";
  message: string;
  details?: string;
  created_at: string;
}

const toast = useAppToast();
const loading = ref(true);
const logs = ref<Log[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 20;

const logColors: Record<string, "info" | "warning" | "error"> = {
  info: "info",
  warning: "warning",
  error: "error",
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const fetchLogs = async () => {
  loading.value = true;
  try {
    // Note: This would need to be implemented in the admin API
    // For now, we'll just show a placeholder
    logs.value = [];
    totalPages.value = 1;
  } catch (error) {
    toast.error("获取日志列表失败");
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchLogs();
});
</script>