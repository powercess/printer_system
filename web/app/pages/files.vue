<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        文件管理
      </h1>
      <UButton
        color="primary"
        icon="i-heroicons-outline-plus"
        @click="showUploadModal = true"
      >
        上传文件
      </UButton>
    </div>

    <!-- Files List -->
    <UCard>
      <div v-if="loading" class="flex justify-center py-8">
        <LoadingSpinner />
      </div>

      <div v-else-if="files.length === 0" class="text-center py-8 text-gray-500">
        <UIcon name="i-heroicons-outline-document" class="w-12 h-12 mx-auto mb-4 opacity-50" />
        <p>暂无文件</p>
        <UButton color="primary" variant="soft" class="mt-4" @click="showUploadModal = true">
          上传文件
        </UButton>
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
              <p class="font-medium">{{ file.displayName }}</p>
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
              color="neutral"
              variant="soft"
              size="sm"
              icon="i-heroicons-outline-eye"
              @click="previewFile(file)"
            >
              预览
            </UButton>
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

    <!-- Upload Modal -->
    <UModal v-model:open="showUploadModal" :ui="{ content: 'sm:max-w-lg' }">
      <template #content>
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold">上传文件</h3>
              <UButton color="neutral" variant="ghost" icon="i-heroicons-solid-x-mark" @click="showUploadModal = false" />
            </div>
          </template>
          <div
            class="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-8 text-center"
            :class="{ 'border-primary bg-primary/5': isDragging }"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="handleDrop"
          >
            <UIcon name="i-heroicons-outline-cloud-arrow-up" class="w-12 h-12 mx-auto text-gray-400 mb-4" />
            <p class="text-gray-600 dark:text-gray-400 mb-2">
              拖拽文件到此处，或
            </p>
            <input
              ref="fileInput"
              type="file"
              class="hidden"
              accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.odt,.ods,.odp,.rtf,.html,.htm,.png,.jpg,.jpeg"
              @change="handleFileSelect"
            >
            <UButton color="primary" variant="soft" @click="fileInput?.click()">
              选择文件
            </UButton>
            <p class="text-sm text-gray-500 mt-2">
              支持 PDF, Word, Excel, PPT, TXT, ODT 等格式
            </p>
          </div>
          <div v-if="selectedUploadFile" class="mt-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <UIcon name="i-heroicons-outline-document" class="w-8 h-8 text-primary" />
                <div>
                  <p class="font-medium">{{ selectedUploadFile.name }}</p>
                  <p class="text-sm text-gray-500">{{ formatFileSize(selectedUploadFile.size) }}</p>
                </div>
              </div>
              <div class="flex items-center gap-2">
                <UButton
                  color="neutral"
                  variant="soft"
                  @click="previewUploadFile"
                >
                  预览
                </UButton>
                <UButton
                  color="primary"
                  :loading="uploading"
                  @click="uploadFile"
                >
                  直接上传
                </UButton>
                <UButton color="neutral" variant="ghost" icon="i-heroicons-solid-x-mark" @click="selectedUploadFile = null" />
              </div>
            </div>
          </div>
        </UCard>
      </template>
    </UModal>

    <!-- PDF Preview Modal -->
    <PdfPreviewModal
      v-model:open="showPreviewModal"
      :file="previewingFile"
      @confirmed="handlePreviewConfirmed"
    />
  </div>
</template>

<script setup lang="ts">
import type { FileInfo } from "../../types/file";
import { useAppToast } from "../../composables/useToast";
import { useFileApi } from "../../api/file";
import { createPageLogger } from "../../utils/logger";
import LoadingSpinner from "../components/common/LoadingSpinner.vue";
import PdfPreviewModal from "../components/common/PdfPreviewModal.vue";

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

// Upload modal state
const showUploadModal = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);
const selectedUploadFile = ref<File | null>(null);
const uploading = ref(false);

// Preview modal state
const showPreviewModal = ref(false);
const previewingFile = ref<File | null>(null);

const fileApi = useFileApi();

const getFileIcon = (type: string) => {
  if (type?.includes("pdf")) return "i-heroicons-outline-document-text";
  if (type?.includes("word") || type?.includes("document")) return "i-heroicons-outline-document-text";
  if (type?.includes("image")) return "i-heroicons-outline-photo";
  return "i-heroicons-outline-document";
};

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
  log.userAction("打印文件", { fileId: file.id, fileName: file.displayName });
  navigateTo(`/?file=${file.id}`);
};

const confirmDelete = (file: FileInfo) => {
  log.debug("确认删除文件", { fileId: file.id, fileName: file.displayName });
  fileToDelete.value = file;
  deleteModalOpen.value = true;
};

const deleteFile = async () => {
  if (!fileToDelete.value) return;

  log.userAction("删除文件", { fileId: fileToDelete.value.id, fileName: fileToDelete.value.displayName });
  deleting.value = true;
  try {
    await fileApi.delete(fileToDelete.value.id);
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

// Preview file
const previewFile = async (file: FileInfo) => {
  log.userAction("预览文件", { fileId: file.id, fileName: file.displayName });

  try {
    // Get the download URL first
    const result = await fileApi.getDownloadUrl(file.id);
    if (result.downloadUrl) {
      // Open the presigned URL in new tab
      window.open(result.downloadUrl, '_blank');
    }
  } catch (e) {
    log.error("获取预览链接失败", e);
    toast.error("预览失败，请重试");
  }
};

// Handle file drop for upload
const handleDrop = (e: DragEvent) => {
  isDragging.value = false;
  const droppedFiles = e.dataTransfer?.files;
  if (droppedFiles?.length && droppedFiles[0]) {
    log.userAction("拖拽上传文件", { fileName: droppedFiles[0].name });
    selectUploadFile(droppedFiles[0]);
  }
};

// Handle file select for upload
const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement;
  const selectedFiles = target.files;
  if (selectedFiles?.length && selectedFiles[0]) {
    log.userAction("选择上传文件", { fileName: selectedFiles[0].name });
    selectUploadFile(selectedFiles[0]);
  }
};

// Select file for upload (validate first)
const selectUploadFile = (file: File) => {
  const ext = file.name.split('.').pop()?.toLowerCase() || '';
  const allowedExtensions = ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'odt', 'ods', 'odp', 'rtf', 'html', 'htm', 'png', 'jpg', 'jpeg'];

  if (!allowedExtensions.includes(ext)) {
    log.warn("文件类型不支持", { ext });
    toast.error("不支持的文件格式");
    return;
  }

  log.debug("选择上传文件", { fileName: file.name, fileSize: file.size });
  selectedUploadFile.value = file;
};

// Preview file before upload
const previewUploadFile = () => {
  if (!selectedUploadFile.value) return;
  log.userAction("预览上传文件", { fileName: selectedUploadFile.value.name });
  previewingFile.value = selectedUploadFile.value;
  showPreviewModal.value = true;
};

// Upload file directly
const uploadFile = async () => {
  if (!selectedUploadFile.value) return;

  uploading.value = true;
  try {
    log.loadStart("文件上传");
    const result = await fileApi.upload(selectedUploadFile.value);
    log.loadSuccess("文件上传", { fileId: result.fileId });
    toast.success("文件上传成功");
    showUploadModal.value = false;
    selectedUploadFile.value = null;
    await fetchFiles();
  } catch (error) {
    log.loadError("文件上传", error);
    toast.error("文件上传失败");
  } finally {
    uploading.value = false;
  }
};

// Handle preview confirmed
const handlePreviewConfirmed = async (fileId: number, fileName: string) => {
  log.success("预览确认完成", { fileId, fileName });
  showUploadModal.value = false;
  selectedUploadFile.value = null;
  await fetchFiles();
  toast.success("文件已保存");
};

onMounted(() => {
  log.mounted();
  fetchFiles();
});
</script>