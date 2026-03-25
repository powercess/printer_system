<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        文件管理
      </h1>
    </div>

    <!-- Files Table -->
    <UCard>
      <UTable :data="files" :columns="columns">
        <template #fileSize-cell="{ row }">
          {{ formatFileSize(row.original.fileSize) }}
        </template>

        <template #uploadTime-cell="{ row }">
          {{ formatDate(row.original.uploadTime) }}
        </template>

        <template #actions-cell="{ row }">
          <UButton
            color="error"
            variant="ghost"
            size="sm"
            icon="i-heroicons-outline-trash"
            @click="confirmDelete(row.original)"
          />
        </template>
      </UTable>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <Pagination
          v-model:current-page="currentPage"
          :total-pages="totalPages"
          @change="fetchFiles"
        />
      </div>
    </UCard>

    <!-- Delete Confirmation Modal -->
    <UModal v-model:open="deleteModalOpen">
      <template #content>
        <UCard>
          <div class="text-center">
            <UIcon name="i-heroicons-solid-exclamation-triangle" class="w-12 h-12 mx-auto text-red-500 mb-4" />
            <h3 class="text-lg font-semibold mb-2">确认删除</h3>
            <p class="text-gray-500 mb-6">
              确定要删除文件 "{{ fileToDelete?.displayName }}" 吗？此操作不可撤销。
            </p>
            <div class="flex justify-center gap-3">
              <UButton color="neutral" variant="ghost" @click="deleteModalOpen = false">
                取消
              </UButton>
              <UButton color="error" :loading="deleting" @click="deleteFile">
                删除
              </UButton>
            </div>
          </div>
        </UCard>
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import type { FileInfo } from "../../../types/file";
import { useAppToast } from "../../../composables/useToast";
import { useAdminApi } from "../../../api/admin";
import { useFileApi } from "../../../api/file";

definePageMeta({
  middleware: ["auth", "admin"],
});

const toast = useAppToast();
const adminApi = useAdminApi();
const fileApi = useFileApi();
const files = ref<FileInfo[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 10;

const deleteModalOpen = ref(false);
const fileToDelete = ref<FileInfo | null>(null);
const deleting = ref(false);

const columns = [
  { accessorKey: "id", header: "ID" },
  { accessorKey: "displayName", header: "文件名" },
  { accessorKey: "userId", header: "用户ID" },
  { accessorKey: "fileSize", header: "大小" },
  { accessorKey: "pageCount", header: "页数" },
  { accessorKey: "uploadTime", header: "上传时间" },
  {
    id: "actions",
    header: "操作",
    cell: () => null,
  },
];

const formatFileSize = (bytes: number) => {
  if (!bytes) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const fetchFiles = async () => {
  try {
    const result = await adminApi.getFileList({
      page: currentPage.value,
      page_size: pageSize,
    });
    files.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
  } catch (error) {
    toast.error("获取文件列表失败");
  }
};

const confirmDelete = (file: FileInfo) => {
  fileToDelete.value = file;
  deleteModalOpen.value = true;
};

const deleteFile = async () => {
  if (!fileToDelete.value) return;

  deleting.value = true;
  try {
    await fileApi.delete(fileToDelete.value.id);
    toast.success("文件已删除");
    await fetchFiles();
  } catch (error) {
    toast.error("删除文件失败");
  } finally {
    deleting.value = false;
    deleteModalOpen.value = false;
    fileToDelete.value = null;
  }
};

onMounted(() => {
  fetchFiles();
});
</script>