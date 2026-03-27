<template>
  <NuxtLayout name="admin">
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          文件管理
        </h1>
        <div class="flex gap-2">
          <UButton
            v-if="selectedIds.length > 0"
            color="error"
            variant="outline"
            icon="i-heroicons-outline-trash"
            @click="confirmBatchDelete"
          >
            批量删除 ({{ selectedIds.length }})
          </UButton>
          <UButton
            color="primary"
            variant="outline"
            icon="i-heroicons-outline-arrow-path"
            @click="fetchFiles"
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
            placeholder="搜索文件名或用户ID..."
            icon="i-heroicons-outline-magnifying-glass"
            class="flex-1 min-w-[200px]"
            @keyup.enter="fetchFiles"
          />
          <UButton color="primary" @click="fetchFiles">搜索</UButton>
        </div>
      </UCard>

      <!-- Batch Actions Bar -->
      <div v-if="selectedIds.length > 0" class="flex items-center gap-4 p-3 bg-primary/10 rounded-lg">
        <UCheckbox
          :model-value="selectedIds.length === files.length && files.length > 0"
          :indeterminate="selectedIds.length > 0 && selectedIds.length < files.length"
          @update:model-value="(val: boolean | 'indeterminate') => toggleSelectAll(val === true)"
        />
        <span class="text-sm">已选择 {{ selectedIds.length }} 项</span>
        <UButton color="neutral" variant="ghost" size="sm" @click="selectedIds = []">
          取消选择
        </UButton>
        <div class="flex-1" />
        <UButton color="error" variant="outline" size="sm" icon="i-heroicons-outline-trash" @click="confirmBatchDelete">
          批量删除
        </UButton>
      </div>

      <!-- Files Table -->
      <UCard>
        <UTable :data="files" :columns="columns" v-model:sort="sort">
          <template #select-header>
            <UCheckbox
              :model-value="selectedIds.length === files.length && files.length > 0"
              :indeterminate="selectedIds.length > 0 && selectedIds.length < files.length"
              @update:model-value="(val: boolean | 'indeterminate') => toggleSelectAll(val === true)"
            />
          </template>

          <template #select-cell="{ row }">
            <UCheckbox
              :model-value="selectedIds.includes(row.original.id)"
              @update:model-value="(val: boolean | 'indeterminate') => toggleSelect(row.original.id, val === true)"
            />
          </template>

          <template #id-cell="{ row }">
            <span class="font-mono text-sm">#{{ row.original.id }}</span>
          </template>

          <template #file-cell="{ row }">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-lg bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                <UIcon :name="getFileIcon(row.original.fileType)" class="w-5 h-5 text-gray-500" />
              </div>
              <div class="max-w-[200px]">
                <p class="font-medium truncate">{{ row.original.displayName }}</p>
                <p class="text-sm text-gray-500">{{ row.original.fileType?.toUpperCase() }}</p>
              </div>
            </div>
          </template>

          <template #user-cell="{ row }">
            <div>
              <p class="font-medium">用户 {{ row.original.userId }}</p>
            </div>
          </template>

          <template #size-cell="{ row }">
            {{ formatFileSize(row.original.fileSize) }}
          </template>

          <template #pages-cell="{ row }">
            <UBadge color="neutral" variant="subtle">
              {{ row.original.pageCount }} 页
            </UBadge>
          </template>

          <template #upload_time-cell="{ row }">
            {{ formatDate(row.original.uploadTime) }}
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
                color="primary"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-arrow-down-tray"
                @click="downloadFile(row.original)"
              />
              <UButton
                color="error"
                variant="ghost"
                size="xs"
                icon="i-heroicons-outline-trash"
                @click="confirmDelete(row.original)"
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
            @update:page="fetchFiles"
          />
        </div>
      </UCard>

      <!-- File Detail Modal -->
      <UModal v-model:open="detailModalOpen">
        <template #content>
          <UCard>
            <template #header>
              <h3 class="text-lg font-semibold">文件详情</h3>
            </template>

            <div v-if="selectedFile" class="space-y-4">
              <div class="flex items-center gap-4">
                <div class="w-16 h-16 rounded-lg bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
                  <UIcon :name="getFileIcon(selectedFile.fileType)" class="w-8 h-8 text-gray-500" />
                </div>
                <div>
                  <p class="text-xl font-semibold">{{ selectedFile.displayName }}</p>
                  <p class="text-gray-500">{{ selectedFile.fileType?.toUpperCase() }}</p>
                </div>
              </div>

              <div class="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p class="text-gray-500">文件ID</p>
                  <p class="font-medium">#{{ selectedFile.id }}</p>
                </div>
                <div>
                  <p class="text-gray-500">所属用户</p>
                  <p class="font-medium">用户 {{ selectedFile.userId }}</p>
                </div>
                <div>
                  <p class="text-gray-500">文件大小</p>
                  <p class="font-medium">{{ formatFileSize(selectedFile.fileSize) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">页数</p>
                  <p class="font-medium">{{ selectedFile.pageCount }} 页</p>
                </div>
                <div>
                  <p class="text-gray-500">上传时间</p>
                  <p class="font-medium">{{ formatDate(selectedFile.uploadTime) }}</p>
                </div>
                <div>
                  <p class="text-gray-500">Blob ID</p>
                  <p class="font-medium font-mono text-sm">{{ selectedFile.blobId }}</p>
                </div>
              </div>
            </div>

            <template #footer>
              <div class="flex justify-end gap-3">
                <UButton color="neutral" variant="ghost" @click="detailModalOpen = false">
                  关闭
                </UButton>
                <UButton color="primary" icon="i-heroicons-outline-arrow-down-tray" @click="downloadFile(selectedFile!)">
                  下载
                </UButton>
              </div>
            </template>
          </UCard>
        </template>
      </UModal>

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

      <!-- Batch Delete Confirmation Modal -->
      <UModal v-model:open="batchDeleteModalOpen">
        <template #content>
          <UCard>
            <div class="text-center">
              <UIcon name="i-heroicons-solid-exclamation-triangle" class="w-12 h-12 mx-auto text-red-500 mb-4" />
              <h3 class="text-lg font-semibold mb-2">确认批量删除</h3>
              <p class="text-gray-500 mb-6">
                确定要删除选中的 {{ selectedIds.length }} 个文件吗？此操作不可撤销。
              </p>
              <div class="flex justify-center gap-3">
                <UButton color="neutral" variant="ghost" @click="batchDeleteModalOpen = false">
                  取消
                </UButton>
                <UButton color="error" :loading="batchDeleting" @click="batchDeleteFiles">
                  删除
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
const total = ref(0);
const pageSize = 10;
const searchQuery = ref("");
const sort = ref({ column: "uploadTime", direction: "desc" as const });

// Selection
const selectedIds = ref<number[]>([]);

const detailModalOpen = ref(false);
const deleteModalOpen = ref(false);
const batchDeleteModalOpen = ref(false);
const deleting = ref(false);
const batchDeleting = ref(false);

const selectedFile = ref<FileInfo | null>(null);
const fileToDelete = ref<FileInfo | null>(null);

const columns = [
  { id: "select", header: "" },
  { id: "id", header: "ID", sortable: true },
  { id: "file", header: "文件", sortable: true },
  { id: "user", header: "用户", sortable: true },
  { id: "size", header: "大小", sortable: true },
  { id: "pages", header: "页数" },
  { accessorKey: "uploadTime", header: "上传时间", sortable: true },
  { id: "actions", header: "操作" },
];

const getFileIcon = (fileType?: string) => {
  if (!fileType) return "i-heroicons-outline-document";
  const type = fileType.toLowerCase();
  if (type === "pdf") return "i-heroicons-outline-document-text";
  if (["doc", "docx"].includes(type)) return "i-heroicons-outline-document-text";
  if (["xls", "xlsx"].includes(type)) return "i-heroicons-outline-table-cells";
  if (["jpg", "jpeg", "png", "gif"].includes(type)) return "i-heroicons-outline-photo";
  return "i-heroicons-outline-document";
};

const formatFileSize = (bytes?: number) => {
  if (!bytes) return "0 B";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

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
    selectedIds.value = files.value.map(f => f.id);
  } else {
    selectedIds.value = [];
  }
};

const fetchFiles = async () => {
  try {
    const result = await adminApi.getFileList({
      page: currentPage.value,
      pageSize: pageSize,
    });
    files.value = result.items;
    total.value = result.total;
    totalPages.value = Math.ceil(result.total / pageSize);
    selectedIds.value = [];
  } catch (error) {
    toast.error("获取文件列表失败");
  }
};

const openDetailModal = (file: FileInfo) => {
  selectedFile.value = file;
  detailModalOpen.value = true;
};

const downloadFile = async (file: FileInfo) => {
  try {
    const result = await fileApi.getDownloadUrl(file.id);
    if (result.downloadUrl) {
      window.open(result.downloadUrl, "_blank");
    }
  } catch (error) {
    toast.error("获取下载链接失败");
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

const confirmBatchDelete = () => {
  if (selectedIds.value.length === 0) return;
  batchDeleteModalOpen.value = true;
};

const batchDeleteFiles = async () => {
  batchDeleting.value = true;
  try {
    let successCount = 0;
    let failCount = 0;
    for (const fileId of selectedIds.value) {
      try {
        await fileApi.delete(fileId);
        successCount++;
      } catch {
        failCount++;
      }
    }
    if (successCount > 0) {
      toast.success(`成功删除 ${successCount} 个文件${failCount > 0 ? `，${failCount} 个失败` : ""}`);
    } else {
      toast.error("删除失败");
    }
    selectedIds.value = [];
    batchDeleteModalOpen.value = false;
    await fetchFiles();
  } finally {
    batchDeleting.value = false;
  }
};

onMounted(() => {
  fetchFiles();
});
</script>