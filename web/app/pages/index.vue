<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        自助打印
      </h1>
    </div>

    <!-- Printer Status -->
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">打印机状态</h2>
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-heroicons-outline-arrow-path"
            :loading="loadingPrinters"
            @click="refreshPrinters"
          >
            刷新
          </UButton>
        </div>
      </template>
      <div v-if="loadingPrinters" class="flex justify-center py-4">
        <LoadingSpinner />
      </div>
      <div v-else-if="printers.length === 0" class="text-center py-4">
        <UIcon name="i-heroicons-outline-printer" class="w-12 h-12 mx-auto text-gray-400 mb-2" />
        <p class="text-gray-500">暂无可用打印机</p>
        <p class="text-sm text-gray-400 mt-1">您可以先上传文件，待打印机可用后再打印</p>
      </div>
      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <div
          v-for="printer in printers"
          :key="printer.name"
          class="flex items-center gap-3 p-3 rounded-lg border border-gray-200 dark:border-gray-700"
        >
          <div
            class="w-3 h-3 rounded-full"
            :class="statusColors[printer.state || 'stopped']"
          />
          <div>
            <p class="font-medium">{{ printer.name }}</p>
            <p class="text-sm text-gray-500">{{ printer.description }}</p>
          </div>
        </div>
      </div>
    </UCard>

    <!-- File Selection -->
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">选择文件</h2>
          <div class="flex gap-2">
            <UButton
              :color="fileSource === 'existing' ? 'primary' : 'neutral'"
              variant="soft"
              size="sm"
              @click="switchToExisting"
            >
              已上传文件
            </UButton>
            <UButton
              :color="fileSource === 'new' ? 'primary' : 'neutral'"
              variant="soft"
              size="sm"
              @click="switchToNew"
            >
              上传新文件
            </UButton>
          </div>
        </div>
      </template>

      <!-- Existing Files Selection -->
      <div v-if="fileSource === 'existing'">
        <div v-if="loadingExistingFiles" class="flex justify-center py-4">
          <LoadingSpinner />
        </div>
        <div v-else-if="existingFiles.length === 0" class="text-center py-8 text-gray-500">
          <UIcon name="i-heroicons-outline-document" class="w-12 h-12 mx-auto mb-4 opacity-50" />
          <p>暂无已上传的文件</p>
          <UButton color="primary" variant="soft" class="mt-4" @click="switchToNew">
            上传新文件
          </UButton>
        </div>
        <div v-else class="space-y-2 max-h-64 overflow-y-auto">
          <div
            v-for="file in existingFiles"
            :key="file.id"
            class="flex items-center justify-between p-3 rounded-lg border cursor-pointer transition-colors"
            :class="selectedExistingFile?.id === file.id
              ? 'border-primary bg-primary/5'
              : 'border-gray-200 dark:border-gray-700 hover:border-gray-300'"
            @click="selectExistingFile(file)"
          >
            <div class="flex items-center gap-3">
              <UIcon :name="getFileIcon(file.fileType)" class="w-8 h-8 text-primary" />
              <div>
                <p class="font-medium">{{ file.displayName }}</p>
                <div class="flex items-center gap-2 text-sm text-gray-500">
                  <span>{{ formatFileSize(file.fileSize) }}</span>
                  <span>·</span>
                  <span>{{ file.pageCount }} 页</span>
                </div>
              </div>
            </div>
            <UIcon
              v-if="selectedExistingFile?.id === file.id"
              name="i-heroicons-solid-check-circle"
              class="w-5 h-5 text-primary"
            />
          </div>
        </div>

        <!-- Selected file action for existing files -->
        <div v-if="selectedExistingFile" class="mt-4 p-4 bg-primary/5 rounded-lg border border-primary/20">
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium text-primary">已选择: {{ selectedExistingFile.displayName }}</p>
              <p class="text-sm text-gray-500">{{ selectedExistingFile.pageCount }} 页 · {{ formatFileSize(selectedExistingFile.fileSize) }}</p>
            </div>
            <UButton color="neutral" variant="ghost" size="sm" @click="selectedExistingFile = null">
              取消选择
            </UButton>
          </div>
        </div>
      </div>

      <!-- Upload New File -->
      <div v-else>
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
            accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg"
            @change="handleFileSelect"
          >
          <UButton color="primary" variant="soft" @click="fileInput?.click()">
            选择文件
          </UButton>
          <p class="text-sm text-gray-500 mt-2">
            支持 PDF, Word, TXT, PNG, JPG 格式
          </p>
        </div>

        <!-- Selected File (New Upload) -->
        <div v-if="selectedNewFile" class="mt-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-3">
              <UIcon name="i-heroicons-outline-document" class="w-8 h-8 text-primary" />
              <div>
                <p class="font-medium">{{ selectedNewFile.name }}</p>
                <p class="text-sm text-gray-500">{{ formatFileSize(selectedNewFile.size) }}</p>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <UButton
                color="primary"
                :loading="uploading"
                @click="uploadAndSelect"
              >
                上传文件
              </UButton>
              <UButton color="neutral" variant="ghost" icon="i-heroicons-solid-x-mark" @click="clearNewFile" />
            </div>
          </div>
        </div>
      </div>

      <!-- Upload Progress -->
      <div v-if="uploading" class="mt-4">
        <UProgress :value="uploadProgress" />
        <p class="text-sm text-gray-500 text-center mt-2">正在上传...</p>
      </div>
    </UCard>

    <!-- Selected File Info & Actions (when file is ready) -->
    <UCard v-if="readyFileInfo">
      <template #header>
        <h2 class="text-lg font-semibold">已选择文件</h2>
      </template>
      <div class="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-800 rounded-lg">
        <div class="flex items-center gap-3">
          <UIcon :name="getFileIcon(readyFileInfo.fileType)" class="w-10 h-10 text-primary" />
          <div>
            <p class="font-medium">{{ readyFileInfo.displayName }}</p>
            <div class="flex items-center gap-2 text-sm text-gray-500">
              <span>{{ formatFileSize(readyFileInfo.fileSize) }}</span>
              <span>·</span>
              <span>{{ readyFileInfo.pageCount }} 页</span>
            </div>
          </div>
        </div>
        <UButton color="neutral" variant="ghost" @click="clearSelection">
          重新选择
        </UButton>
      </div>
    </UCard>

    <!-- Print Options -->
    <UCard v-if="readyFileInfo && printers.length > 0">
      <template #header>
        <h2 class="text-lg font-semibold">打印选项</h2>
      </template>
      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Printer Selection -->
        <UFormField label="选择打印机" required>
          <USelect
            v-model="printOptions.printer"
            :items="printerOptions"
            placeholder="请选择打印机"
          />
        </UFormField>

        <!-- Copies -->
        <UFormField label="打印份数">
          <UInput v-model.number="printOptions.copies" type="number" min="1" max="100" />
        </UFormField>

        <!-- Color Mode -->
        <UFormField label="颜色模式">
          <URadioGroup v-model="printOptions.colorMode" :items="colorModeOptions" />
        </UFormField>

        <!-- Paper Size -->
        <UFormField label="纸张大小">
          <USelect
            v-model="printOptions.paperSize"
            :items="paperSizeOptions"
          />
        </UFormField>

        <!-- Duplex -->
        <UFormField label="双面打印">
          <USwitch v-model="printOptions.duplex" />
        </UFormField>
      </div>

      <!-- Price Estimate -->
      <div v-if="estimatedPrice" class="mt-6 p-4 bg-primary/10 rounded-lg">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-600">预估价格</p>
            <p class="text-2xl font-bold text-primary">¥{{ estimatedPrice.finalAmount.toFixed(2) }}</p>
          </div>
          <div class="text-right">
            <p class="text-sm text-gray-500">共 {{ estimatedPrice.pageCount }} 页</p>
            <p v-if="estimatedPrice.discountAmount > 0" class="text-xs text-green-600">
              已优惠 ¥{{ estimatedPrice.discountAmount.toFixed(2) }}
            </p>
          </div>
        </div>
      </div>

      <div class="mt-6 flex justify-end gap-3">
        <UButton color="neutral" variant="ghost" :loading="estimating" @click="estimatePrice">
          估算价格
        </UButton>
        <UButton color="primary" :loading="submitting" @click="submitOrder">
          提交订单
        </UButton>
      </div>
    </UCard>

    <!-- No Printer Notice -->
    <UCard v-if="readyFileInfo && printers.length === 0">
      <div class="text-center py-4">
        <UIcon name="i-heroicons-outline-printer" class="w-12 h-12 mx-auto text-yellow-500 mb-4" />
        <p class="text-gray-600">文件已准备好，但暂无可用打印机</p>
        <p class="text-sm text-gray-500 mt-1">请稍后刷新打印机列表，或前往<NuxtLink to="/files" class="text-primary hover:underline">文件管理</NuxtLink>查看</p>
      </div>
    </UCard>

    <!-- Payment Modal -->
    <UModal v-model:open="showPaymentModal" :ui="{ content: 'sm:max-w-md' }">
      <template #content>
        <UCard>
          <template #header>
            <div class="flex items-center justify-between">
              <h3 class="text-lg font-semibold">确认支付</h3>
              <UButton color="neutral" variant="ghost" icon="i-heroicons-solid-x-mark" @click="showPaymentModal = false" />
            </div>
          </template>
          <div class="space-y-4">
            <div class="text-center">
              <p class="text-sm text-gray-500">支付金额</p>
              <p class="text-3xl font-bold text-primary">¥{{ orderInfo?.finalAmount.toFixed(2) }}</p>
            </div>
            <div class="space-y-2">
              <p class="text-sm text-gray-500">选择支付方式</p>
              <div class="grid grid-cols-3 gap-2">
                <UButton
                  :color="selectedPaymentMethod === 'wallet' ? 'primary' : 'neutral'"
                  variant="outline"
                  class="justify-center"
                  @click="selectedPaymentMethod = 'wallet'"
                >
                  钱包余额
                </UButton>
                <UButton
                  :color="selectedPaymentMethod === 'wechat' ? 'primary' : 'neutral'"
                  variant="outline"
                  class="justify-center"
                  @click="selectedPaymentMethod = 'wechat'"
                >
                  微信支付
                </UButton>
                <UButton
                  :color="selectedPaymentMethod === 'alipay' ? 'primary' : 'neutral'"
                  variant="outline"
                  class="justify-center"
                  @click="selectedPaymentMethod = 'alipay'"
                >
                  支付宝
                </UButton>
              </div>
              <p v-if="selectedPaymentMethod === 'wallet'" class="text-xs text-gray-500 text-center mt-2">
                当前余额: ¥{{ userStore.balance.toFixed(2) }}
              </p>
            </div>
          </div>
          <template #footer>
            <div class="flex justify-end gap-2">
              <UButton color="neutral" variant="ghost" @click="showPaymentModal = false">
                取消
              </UButton>
              <UButton color="primary" :loading="paying" @click="confirmPayment">
                确认支付
              </UButton>
            </div>
          </template>
        </UCard>
      </template>
    </UModal>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from "../../stores/user";
import { useAppToast } from "../../composables/useToast";
import { useFileApi } from "../../api/file";
import { useOrderApi } from "../../api/order";
import { usePrinterApi } from "../../api/printer";
import { usePaymentApi } from "../../api/payment";
import { createPageLogger } from "../../utils/logger";
import LoadingSpinner from "../components/common/LoadingSpinner.vue";
import type { CupsPrinter } from "../../types/printer";
import type { PriceEstimate } from "../../types/api";
import type { CreateOrderResponse } from "../../types/order";
import type { PaymentMethod, WalletPaymentResponse, ThirdPartyPaymentResponse } from "../../types/payment";
import type { FileInfo } from "../../types/file";

const log = createPageLogger("index");

definePageMeta({
  middleware: ["auth"],
});

const toast = useAppToast();
const userStore = useUserStore();
const route = useRoute();

// File selection state
const fileSource = ref<'existing' | 'new'>('new');
const fileInput = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);
const selectedNewFile = ref<File | null>(null);
const selectedExistingFile = ref<FileInfo | null>(null);
const uploading = ref(false);
const uploadProgress = ref(0);

// Existing files
const loadingExistingFiles = ref(false);
const existingFiles = ref<FileInfo[]>([]);

// Printer state
const loadingPrinters = ref(false);
const printers = ref<CupsPrinter[]>([]);

// Order state
const submitting = ref(false);
const estimating = ref(false);
const paying = ref(false);
const estimatedPrice = ref<PriceEstimate | null>(null);

// Payment modal state
const showPaymentModal = ref(false);
const selectedPaymentMethod = ref<PaymentMethod>("wallet");
const orderInfo = ref<CreateOrderResponse | null>(null);

const printOptions = reactive({
  printer: "",
  copies: 1,
  colorMode: "bw" as "bw" | "color",
  paperSize: "A4",
  duplex: false,
});

const statusColors: Record<string, string> = {
  idle: "bg-green-500",
  printing: "bg-yellow-500",
  stopped: "bg-red-500",
};

const colorModeOptions = [
  { value: "bw", label: "黑白" },
  { value: "color", label: "彩色" },
];

const paperSizeOptions = [
  { value: "A4", label: "A4" },
  { value: "A3", label: "A3" },
  { value: "A5", label: "A5" },
  { value: "Letter", label: "Letter" },
];

const printerOptions = computed(() =>
  printers.value.map((p) => ({ value: p.name, label: p.name })),
);

// Ready file info (unified for both existing and newly uploaded)
const readyFileInfo = computed(() => selectedExistingFile.value);

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

// Fetch existing files
const fetchExistingFiles = async () => {
  loadingExistingFiles.value = true;
  try {
    const fileApi = useFileApi();
    const result = await fileApi.getList({ page: 1, page_size: 50 });
    existingFiles.value = result.items;
  } catch (error) {
    log.error("获取文件列表失败", error);
  } finally {
    loadingExistingFiles.value = false;
  }
};

// Switch to existing files tab
const switchToExisting = async () => {
  fileSource.value = 'existing';
  selectedNewFile.value = null;
  await fetchExistingFiles();
};

// Switch to new file tab
const switchToNew = () => {
  fileSource.value = 'new';
  selectedExistingFile.value = null;
  estimatedPrice.value = null;
};

// Select existing file
const selectExistingFile = (file: FileInfo) => {
  selectedExistingFile.value = file;
  selectedNewFile.value = null;
  estimatedPrice.value = null;
  log.debug("选择已上传文件", { fileId: file.id, fileName: file.displayName });
};

// Clear selection
const clearSelection = () => {
  selectedExistingFile.value = null;
  selectedNewFile.value = null;
  estimatedPrice.value = null;
  fileSource.value = 'new';
};

// Clear new file selection
const clearNewFile = () => {
  selectedNewFile.value = null;
  if (fileInput.value) {
    fileInput.value.value = "";
  }
};

// Handle file drop
const handleDrop = (e: DragEvent) => {
  isDragging.value = false;
  const files = e.dataTransfer?.files;
  if (files?.length && files[0]) {
    log.userAction("拖拽上传文件", { fileName: files[0].name });
    selectNewFile(files[0]);
  }
};

// Handle file select
const handleFileSelect = (e: Event) => {
  const target = e.target as HTMLInputElement;
  const files = target.files;
  if (files?.length && files[0]) {
    log.userAction("选择上传文件", { fileName: files[0].name });
    selectNewFile(files[0]);
  }
};

// Select new file (validate first)
const selectNewFile = (file: File) => {
  const allowedTypes = [
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/plain",
    "image/png",
    "image/jpeg",
  ];

  if (!allowedTypes.includes(file.type)) {
    log.warn("文件类型不支持", { fileType: file.type });
    toast.error("不支持的文件格式");
    return;
  }

  log.debug("选择新文件", { fileName: file.name, fileSize: file.size, fileType: file.type });
  selectedNewFile.value = file;
  selectedExistingFile.value = null;
  estimatedPrice.value = null;
};

// Upload new file and select it
const uploadAndSelect = async () => {
  if (!selectedNewFile.value) return;

  uploading.value = true;
  uploadProgress.value = 0;

  try {
    log.loadStart("文件上传");
    const fileApi = useFileApi();
    const result = await fileApi.upload(selectedNewFile.value);

    // Create file info from upload result
    const newFile: FileInfo = {
      id: result.fileId,
      userId: 0,
      blobId: 0,
      displayName: selectedNewFile.value.name,
      pageCount: result.pageCount,
      fileSize: result.fileSize,
      fileType: result.fileType,
      storagePath: '',
      uploadTime: new Date().toISOString(),
      createdAt: new Date().toISOString(),
      deletedAt: null,
    };

    // Add to existing files list
    existingFiles.value.unshift(newFile);

    // Select the newly uploaded file
    selectedExistingFile.value = newFile;
    selectedNewFile.value = null;
    fileSource.value = 'existing';

    toast.success("文件上传成功");
    log.success("文件上传成功", { fileId: result.fileId });
  } catch (error) {
    log.error("文件上传失败", error);
    toast.error("文件上传失败");
  } finally {
    uploading.value = false;
    uploadProgress.value = 100;
  }
};

// Estimate price
const estimatePrice = async () => {
  const fileId = selectedExistingFile.value?.id;
  if (!fileId) {
    toast.error("请先选择文件");
    return;
  }

  log.userAction("估算价格", {
    fileId,
    colorMode: printOptions.colorMode,
    duplex: printOptions.duplex,
    paperSize: printOptions.paperSize,
    copies: printOptions.copies,
  });

  estimating.value = true;
  try {
    log.loadStart("价格估算");
    const orderApi = useOrderApi();
    const result = await orderApi.estimate({
      fileId,
      colorMode: printOptions.colorMode === "bw" ? 0 : 1,
      duplex: printOptions.duplex ? 1 : 0,
      paperSize: printOptions.paperSize,
      copies: printOptions.copies,
    });
    estimatedPrice.value = result;
    log.loadSuccess("价格估算", { finalAmount: result.finalAmount, pageCount: result.pageCount });
  } catch (error) {
    log.loadError("价格估算", error);
    toast.error("价格估算失败");
  } finally {
    estimating.value = false;
  }
};

// Submit order
const submitOrder = async () => {
  const fileId = selectedExistingFile.value?.id;
  if (!fileId) {
    toast.error("请先选择文件");
    return;
  }

  if (!printOptions.printer) {
    toast.error("请选择打印机");
    return;
  }

  if (printers.value.length === 0) {
    toast.error("暂无可用打印机");
    return;
  }

  log.formSubmit("打印订单", {
    fileId,
    printer: printOptions.printer,
    copies: printOptions.copies,
    colorMode: printOptions.colorMode,
    duplex: printOptions.duplex,
    paperSize: printOptions.paperSize,
  });

  submitting.value = true;

  try {
    log.loadStart("创建订单");
    const orderApi = useOrderApi();
    const order = await orderApi.create({
      fileId,
      printerName: printOptions.printer,
      copies: printOptions.copies,
      colorMode: printOptions.colorMode === "bw" ? 0 : 1,
      duplex: printOptions.duplex ? 1 : 0,
      paperSize: printOptions.paperSize,
    });

    orderInfo.value = order;
    log.loadSuccess("创建订单", { orderId: order.orderId, finalAmount: order.finalAmount });

    // Show payment modal
    showPaymentModal.value = true;
  } catch (error) {
    log.loadError("创建订单", error);
    const errorMsg = (error as { message?: string })?.message || "订单创建失败";
    toast.error(errorMsg);
  } finally {
    submitting.value = false;
  }
};

// Confirm payment
const confirmPayment = async () => {
  if (!orderInfo.value) return;

  if (selectedPaymentMethod.value === "wallet" && orderInfo.value.finalAmount > userStore.balance) {
    toast.error("余额不足，请先充值");
    return;
  }

  paying.value = true;
  try {
    log.loadStart("支付订单");
    const paymentApi = usePaymentApi();
    const result = await paymentApi.create({
      orderId: orderInfo.value.orderId,
      paymentMethod: selectedPaymentMethod.value,
    });

    // Check if wallet payment
    if ("printSuccess" in result) {
      // Wallet payment completed
      const walletResult = result as WalletPaymentResponse;
      if (walletResult.printSuccess) {
        log.loadSuccess("支付并打印成功");
        toast.success("支付成功，打印任务已提交");
      } else {
        log.loadSuccess("支付成功，打印失败");
        toast.warning("支付成功，但打印任务提交失败: " + (walletResult.printMessage || "未知错误"));
      }
      showPaymentModal.value = false;
      await userStore.fetchBalance();
      resetForm();
    } else {
      // Third-party payment - open payment page in new window
      log.loadSuccess("打开支付页面");
      const thirdPartyResult = result as ThirdPartyPaymentResponse;
      showPaymentModal.value = false;

      if (thirdPartyResult.payurl) {
        // 在当前页面跳转到支付页面
        window.location.href = thirdPartyResult.payurl;
      }
    }
  } catch (error) {
    log.loadError("支付订单", error);
    const errorMsg = (error as { message?: string })?.message || "支付失败";
    toast.error(errorMsg);
  } finally {
    paying.value = false;
  }
};

// Reset form
const resetForm = () => {
  selectedExistingFile.value = null;
  selectedNewFile.value = null;
  estimatedPrice.value = null;
  printOptions.printer = "";
  printOptions.copies = 1;
  printOptions.colorMode = "bw";
  printOptions.paperSize = "A4";
  printOptions.duplex = false;
};

// Refresh printers
const refreshPrinters = async () => {
  loadingPrinters.value = true;
  try {
    log.loadStart("获取打印机列表");
    const printerApi = usePrinterApi();
    const result = await printerApi.getCupsList();
    printers.value = result.items;
    log.loadSuccess("获取打印机列表", { count: printers.value.length });
  } catch (error) {
    log.loadError("获取打印机列表", error);
    toast.error("获取打印机列表失败");
  } finally {
    loadingPrinters.value = false;
  }
};

// Check for file parameter in URL (from files page)
const checkFileParam = async () => {
  const fileId = route.query.file;
  if (fileId) {
    try {
      const fileApi = useFileApi();
      const fileDetail = await fileApi.getDetail(Number(fileId));
      selectedExistingFile.value = fileDetail;
      fileSource.value = 'existing';
      log.debug("从URL参数选择文件", { fileId, fileName: fileDetail.displayName });
    } catch (error) {
      log.error("获取文件详情失败", error);
    }
  }
};

// Initialize
onMounted(async () => {
  log.mounted();
  await Promise.all([
    refreshPrinters(),
    fetchExistingFiles(),
  ]);
  await checkFileParam();
});

onUnmounted(() => {
  log.unmounted();
});
</script>