<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        文件管理
      </h1>
    </div>

    <!-- Files List -->
    <UCard>
      <div v-if="loading" class="flex justify-center py-8">
        <LoadingSpinner />
      </div>

      <div v-else-if="files.length === 0" class="text-center py-8 text-gray-500">
        <UIcon name="i-heroicons-outline-document" class="w-12 h-12 mx-auto mb-4 opacity-50" />
        <p>暂无文件</p>
      </div>

      <div v-else class="space-y-4">
        <div
          v-for="file in files"
          :key="file.id"
          class="flex items-center justify-between p-4 rounded-lg border border-gray-200 dark:border-gray-700 hover:border-primary transition-colors"
        >
          <div class="flex items-center gap-3">
            <UIcon :name="getFileIcon(file.fileType)" class="w-10 h-10 text-primary" />
            <div>
              <p class="font-medium">{{ file.name }}</p>
              <div class="flex items-center gap-2 text-sm text-gray-500">
                <span>{{ formatFileSize(file.fileSize) }}</span>
                <span>·</span>
                <span>{{ file.pageCount }} 页</span>
                <span>·</span>
                <span>{{ formatDate(file.uploadTime) }}</span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2">
            <UButton
              color="primary"
              variant="soft"
              size="sm"
              @click="printFile(file)"
            >
              打印
            </UButton>
            <UButton
              color="error"
              variant="ghost"
              size="sm"
              icon="i-heroicons-outline-trash"
              @click="confirmDelete(file)"
            />
          </div>
        </div>
      </div>

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
              确定要删除文件 "{{ fileToDelete?.name }}" 吗？此操作不可撤销。
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
import type { FileInfo } from "../../types/file";
import { useAppToast } from "../../composables/useToast";
import { useFileApi } from "../../api/file";
import { createPageLogger } from "../../utils/logger";

const log = createPageLogger("files");

definePageMeta({
  middleware: ["auth"],
});

const toast = useAppToast();
const loading = ref(true);
const files = ref<FileInfo[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const pageSize = 10;

const deleteModalOpen = ref(false);
const fileToDelete = ref<FileInfo | null>(null);
const deleting = ref(false);

const fileApi = useFileApi();

const getFileIcon = (type: string) => {
  if (type.includes("pdf")) return "i-heroicons-outline-document-text";
  if (type.includes("word") || type.includes("document")) return "i-heroicons-outline-document-text";
  if (type.includes("image")) return "i-heroicons-outline-photo";
  return "i-heroicons-outline-document";
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const fetchFiles = async () => {
  log.loadStart("文件列表");
  loading.value = true;
  try {
    const result = await fileApi.getList({
      page: currentPage.value,
      page_size: pageSize,
    });
    files.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
    log.loadSuccess("文件列表", { count: result.items.length, total: result.total });
  } catch (error) {
    log.loadError("文件列表", error);
    toast.error("获取文件列表失败");
  } finally {
    loading.value = false;
  }
};

const printFile = (file: FileInfo) => {
  log.userAction("打印文件", { fileId: file.id, fileName: file.name });
  navigateTo(`/?file=${file.id}`);
};

const confirmDelete = (file: FileInfo) => {
  log.debug("确认删除文件", { fileId: file.id, fileName: file.name });
  fileToDelete.value = file;
  deleteModalOpen.value = true;
};

const deleteFile = async () => {
  if (!fileToDelete.value) return;

  log.userAction("删除文件", { fileId: fileToDelete.value.id, fileName: fileToDelete.value.name });
  deleting.value = true;
  try {
    await fileApi.delete(String(fileToDelete.value.id));
    log.success("文件删除成功", { fileId: fileToDelete.value.id });
    toast.success("文件已删除");
    await fetchFiles();
  } catch (error) {
    log.error("文件删除失败", error);
    toast.error("删除失败");
  } finally {
    deleting.value = false;
    deleteModalOpen.value = false;
    fileToDelete.value = null;
  }
};

onMounted(() => {
  log.mounted();
  fetchFiles();
});
</script>