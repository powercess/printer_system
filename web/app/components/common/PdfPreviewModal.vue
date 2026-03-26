<template>
  <UModal
    v-model:open="isOpen"
    fullscreen
    :ui="{
      content: 'w-screen h-screen max-w-none',
      body: 'p-0 h-full flex flex-col',
      header: 'p-4 border-b border-gray-200 dark:border-gray-700',
      footer: 'p-4 border-t border-gray-200 dark:border-gray-700',
    }"
  >
    <template #content>
      <!-- Header -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900">
        <div class="flex items-center gap-3">
          <h3 class="text-xl font-semibold">文件预览</h3>
          <UBadge v-if="previewData?.converted" color="success" variant="soft">
            已转换
          </UBadge>
          <UBadge v-else color="neutral" variant="soft">
            PDF
          </UBadge>
        </div>
        <div class="flex items-center gap-4">
          <span v-if="previewData" class="text-sm text-gray-500">
            {{ previewData.originalFilename }}
            <span v-if="previewData.pageCount"> · {{ previewData.pageCount }} 页</span>
          </span>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-heroicons-solid-x-mark"
            size="lg"
            @click="close"
          />
        </div>
      </div>

      <!-- Content -->
      <div class="flex-1 overflow-hidden relative bg-gray-100 dark:bg-gray-800">
        <!-- Loading State -->
        <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-white/95 dark:bg-gray-900/95 z-10">
          <div class="text-center">
            <UIcon name="i-heroicons-solid-arrow-path" class="w-16 h-16 animate-spin text-primary mb-6" />
            <p class="text-lg text-gray-600 dark:text-gray-400">
              {{ loadingText }}
            </p>
            <UProgress class="w-64 mt-6 mx-auto" :value="null" animation="carousel" />
          </div>
        </div>

        <!-- Error State -->
        <div v-else-if="error" class="absolute inset-0 flex items-center justify-center bg-white dark:bg-gray-900">
          <div class="text-center">
            <UIcon name="i-heroicons-solid-exclamation-triangle" class="w-16 h-16 mx-auto text-red-500 mb-6" />
            <p class="text-lg text-gray-600 dark:text-gray-400">{{ error }}</p>
            <UButton color="primary" size="lg" class="mt-6" @click="retry">
              重试
            </UButton>
          </div>
        </div>

        <!-- PDF Viewer -->
        <embed
          v-else-if="blobUrl"
          :src="blobUrl"
          type="application/pdf"
          class="w-full h-full"
        />
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-between px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900">
        <p class="text-sm text-gray-500">
          <UIcon name="i-heroicons-outline-information-circle" class="w-4 h-4 inline mr-1" />
          预览有效期 {{ remainingMinutes }} 分钟
        </p>
        <div class="flex items-center gap-4">
          <UButton
            color="neutral"
            variant="ghost"
            size="lg"
            @click="close"
          >
            取消
          </UButton>
          <UButton
            color="primary"
            size="lg"
            :loading="confirming"
            :disabled="loading || !!error"
            @click="confirmAndSave"
          >
            确认并保存
          </UButton>
        </div>
      </div>
    </template>
  </UModal>
</template>

<script setup lang="ts">
import type { PreviewUploadResponse } from "../../../types/file";
import { useFileApi } from "../../../api/file";
import { useAppToast } from "../../../composables/useToast";
import { createPageLogger } from "../../../utils/logger";
import { useAuthStore } from "../../../stores/auth";

const log = createPageLogger("PdfPreview");
const toast = useAppToast();
const fileApi = useFileApi();
const authStore = useAuthStore();

const isOpen = defineModel<boolean>("open", { default: false });

const props = defineProps<{
  file?: File | null;
}>();

const emit = defineEmits<{
  confirmed: [fileId: number, fileName: string];
}>();

const loading = ref(false);
const loadingText = ref("准备中...");
const confirming = ref(false);
const error = ref<string | null>(null);
const previewData = ref<PreviewUploadResponse | null>(null);
const blobUrl = ref<string | null>(null);
const remainingMinutes = ref(30);

let expireInterval: ReturnType<typeof setInterval> | null = null;

// Watch for modal open
watch(isOpen, async (open) => {
  if (open && props.file) {
    await startPreview();
  } else if (!open) {
    cleanup();
  }
});

const startPreview = async () => {
  if (!props.file) {
    error.value = "未选择文件";
    return;
  }

  loading.value = true;
  loadingText.value = "正在上传文件...";
  error.value = null;
  previewData.value = null;
  blobUrl.value = null;

  try {
    // Step 1: Upload for preview
    log.loadStart("上传预览文件");
    const result = await fileApi.uploadForPreview(props.file);
    previewData.value = result;
    log.loadSuccess("上传预览文件", { sessionId: result.sessionId, converted: result.converted });

    // Start expiration countdown
    startExpirationCountdown(result.expiresAt);

    // Step 2: Fetch PDF as blob to avoid CORS issues
    loadingText.value = result.converted ? "正在转换PDF..." : "正在加载PDF...";
    await loadPdfAsBlob(result.sessionId);

  } catch (e) {
    log.loadError("上传预览文件", e);
    error.value = (e as Error).message || "文件上传失败";
  } finally {
    loading.value = false;
  }
};

const loadPdfAsBlob = async (sessionId: string) => {
  try {
    const config = useRuntimeConfig();
    const baseUrl = config.public.apiBase;
    const token = authStore.token;

    // Fetch PDF with authentication
    const response = await fetch(`${baseUrl}/api/file/preview/pdf/${sessionId}`, {
      headers: {
        'satoken': token || ''
      }
    });

    if (!response.ok) {
      throw new Error('加载PDF失败');
    }

    // Convert to blob and create URL
    const blob = await response.blob();
    blobUrl.value = URL.createObjectURL(blob);

    log.success("PDF加载成功", { size: blob.size });
  } catch (e) {
    log.error("加载PDF失败", e);
    error.value = "加载PDF失败，请重试";
  }
};

const startExpirationCountdown = (expiresAt: string) => {
  if (expireInterval) {
    clearInterval(expireInterval);
  }

  const updateRemaining = () => {
    const expiry = new Date(expiresAt).getTime();
    const now = Date.now();
    const diffMs = expiry - now;
    remainingMinutes.value = Math.max(0, Math.ceil(diffMs / 60000));
  };

  updateRemaining();
  expireInterval = setInterval(updateRemaining, 60000);
};

const retry = () => {
  if (props.file) {
    startPreview();
  }
};

const confirmAndSave = async () => {
  if (!previewData.value) return;

  confirming.value = true;
  try {
    log.userAction("确认预览保存", { sessionId: previewData.value.sessionId });
    const result = await fileApi.confirmPreview(previewData.value.sessionId);
    log.success("文件保存成功", { fileId: result.fileId });

    toast.success("文件已保存");
    emit("confirmed", result.fileId, previewData.value.originalFilename);
    close();
  } catch (e) {
    log.error("确认保存失败", e);
    toast.error("保存失败，请重试");
  } finally {
    confirming.value = false;
  }
};

const close = () => {
  isOpen.value = false;
};

const cleanup = () => {
  if (expireInterval) {
    clearInterval(expireInterval);
    expireInterval = null;
  }
  // Revoke blob URL to free memory
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value);
    blobUrl.value = null;
  }
  if (previewData.value) {
    // Cancel the preview session on server if not confirmed
    fileApi.cancelPreview(previewData.value.sessionId).catch(() => {});
  }
  loading.value = false;
  error.value = null;
  previewData.value = null;
};

onUnmounted(() => {
  cleanup();
});
</script>