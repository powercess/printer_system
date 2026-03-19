<template>
  <div class="min-h-[60vh] flex items-center justify-center">
    <div v-if="loading" class="text-center">
      <LoadingSpinner />
      <p class="mt-4 text-gray-500">正在查询支付结果...</p>
    </div>

    <!-- 订单支付成功 -->
    <div v-else-if="success && isOrderPayment" class="text-center max-w-md mx-auto">
      <div class="w-20 h-20 mx-auto mb-6 bg-green-100 rounded-full flex items-center justify-center">
        <UIcon name="i-heroicons-solid-check" class="w-10 h-10 text-green-600" />
      </div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-4">支付成功</h1>

      <!-- 订单详情卡片 -->
      <div class="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 text-left mb-4">
        <h3 class="font-semibold text-gray-900 dark:text-white mb-3">订单详情</h3>
        <div class="space-y-2 text-sm">
          <div class="flex justify-between">
            <span class="text-gray-500">订单号</span>
            <span class="font-mono text-gray-900 dark:text-white">{{ orderInfo?.orderId }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">支付金额</span>
            <span class="text-primary font-semibold">¥{{ orderInfo?.finalAmount?.toFixed(2) }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">打印机</span>
            <span class="text-gray-900 dark:text-white">{{ orderInfo?.printerName }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">打印份数</span>
            <span class="text-gray-900 dark:text-white">{{ orderInfo?.copies }} 份</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">颜色模式</span>
            <span class="text-gray-900 dark:text-white">{{ orderInfo?.colorMode === 0 ? '黑白' : '彩色' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">纸张大小</span>
            <span class="text-gray-900 dark:text-white">{{ orderInfo?.paperSize }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">双面打印</span>
            <span class="text-gray-900 dark:text-white">{{ orderInfo?.duplex === 1 ? '是' : '否' }}</span>
          </div>
          <div class="flex justify-between">
            <span class="text-gray-500">支付流水号</span>
            <span class="font-mono text-xs text-gray-600 dark:text-gray-400">{{ paymentId }}</span>
          </div>
        </div>
      </div>

      <!-- 打印状态提示 -->
      <div v-if="printMessage" class="mb-4 p-3 rounded-lg text-sm"
           :class="printSuccess ? 'bg-green-50 text-green-700 dark:bg-green-900/20 dark:text-green-400' : 'bg-orange-50 text-orange-700 dark:bg-orange-900/20 dark:text-orange-400'">
        <div class="flex items-center gap-2">
          <UIcon :name="printSuccess ? 'i-heroicons-solid-check-circle' : 'i-heroicons-solid-exclamation-triangle'" class="w-5 h-5" />
          <span>{{ printMessage }}</span>
        </div>
      </div>

      <!-- 提示信息 -->
      <p class="text-sm text-gray-500 mb-6 px-4">
        如订单存在问题，请携带上述订单信息联系工作人员。
      </p>

      <!-- 操作按钮 -->
      <div class="flex gap-3 justify-center">
        <UButton color="neutral" variant="outline" @click="refreshStatus" :loading="refreshing">
          <UIcon name="i-heroicons-outline-arrow-path" class="w-4 h-4 mr-1" />
          刷新状态
        </UButton>
        <UButton color="primary" @click="goHome">
          返回首页
        </UButton>
      </div>
    </div>

    <!-- 钱包充值成功 -->
    <div v-else-if="success && !isOrderPayment" class="text-center">
      <div class="w-20 h-20 mx-auto mb-6 bg-green-100 rounded-full flex items-center justify-center">
        <UIcon name="i-heroicons-solid-check" class="w-10 h-10 text-green-600" />
      </div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">充值成功</h1>
      <p class="text-gray-500 mb-2">充值金额: ¥{{ amount.toFixed(2) }}</p>
      <p v-if="balance !== null" class="text-gray-500 mb-6">当前余额: ¥{{ balance.toFixed(2) }}</p>
      <UButton color="primary" size="lg" @click="goWallet">
        返回钱包
      </UButton>
    </div>

    <!-- 支付失败/处理中 -->
    <div v-else class="text-center max-w-md mx-auto">
      <div class="w-20 h-20 mx-auto mb-6 bg-red-100 rounded-full flex items-center justify-center">
        <UIcon name="i-heroicons-solid-x-mark" class="w-10 h-10 text-red-600" />
      </div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">支付结果</h1>
      <p class="text-gray-500 mb-2">{{ errorMessage }}</p>
      <p v-if="paymentId" class="text-sm text-gray-400 mb-6">
        支付流水号: <span class="font-mono">{{ paymentId }}</span>
      </p>
      <div class="flex gap-4 justify-center">
        <UButton color="neutral" variant="outline" @click="refreshStatus" :loading="refreshing">
          重新查询
        </UButton>
        <UButton color="primary" @click="goHome">
          返回首页
        </UButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from "../../stores/user";
import { usePaymentApi } from "../../api/payment";
import { useAppToast } from "../../composables/useToast";
import { createPageLogger } from "../../utils/logger";
import LoadingSpinner from "../components/common/LoadingSpinner.vue";
import type { QueryPaymentResponse } from "../../types/payment";

const log = createPageLogger("payment-result");

definePageMeta({
  middleware: ["auth"],
});

const route = useRoute();
const router = useRouter();
const toast = useAppToast();
const userStore = useUserStore();
const paymentApi = usePaymentApi();

const loading = ref(true);
const refreshing = ref(false);
const success = ref(false);
const isOrderPayment = ref(false);
const amount = ref<number>(0);
const balance = ref<number | null>(null);
const paymentId = ref<string>("");
const errorMessage = ref("支付未完成或已取消");
const printSuccess = ref<boolean | undefined>(undefined);
const printMessage = ref<string>("");
const orderInfo = ref<{
  orderId?: number;
  printerName?: string;
  copies?: number;
  colorMode?: number;
  duplex?: number;
  paperSize?: string;
  finalAmount?: number;
} | null>(null);

const outTradeNo = computed(() => route.query.outTradeNo as string);
const tradeStatus = computed(() => route.query.tradeStatus as string);

const processResult = (result: QueryPaymentResponse) => {
  success.value = result.status === 1;
  paymentId.value = result.paymentId;

  if (result.status === 1) {
    amount.value = result.amount || 0;

    if (result.paymentType === "order") {
      isOrderPayment.value = true;
      orderInfo.value = {
        orderId: result.orderId,
        printerName: result.printerName,
        copies: result.copies,
        colorMode: result.colorMode,
        duplex: result.duplex,
        paperSize: result.paperSize,
        finalAmount: result.finalAmount,
      };
      printSuccess.value = result.printSuccess;
      printMessage.value = result.printMessage || (result.printSuccess ? "打印任务已提交，请前往打印机取件" : "打印任务提交中，请稍后查看订单状态");

      if (result.printSuccess) {
        toast.success("支付成功，打印任务已提交");
      } else {
        toast.info("支付成功，请查看打印状态");
      }
    } else {
      isOrderPayment.value = false;
      if (result.balance !== undefined) {
        balance.value = result.balance;
      }
      toast.success("充值成功");
    }
  } else {
    errorMessage.value = result.message || "支付处理中，请稍后查看";
  }
};

const checkPaymentStatus = async (showLoading = true) => {
  if (!outTradeNo.value) {
    log.warn("缺少订单号参数");
    loading.value = false;
    return;
  }

  if (showLoading) {
    loading.value = true;
  } else {
    refreshing.value = true;
  }

  log.info("查询支付结果", { outTradeNo: outTradeNo.value, tradeStatus: tradeStatus.value });

  try {
    const result = await paymentApi.query(outTradeNo.value);
    log.success("查询成功", result);
    processResult(result);

    // 刷新用户余额
    if (result.status === 1) {
      await userStore.fetchBalance();
    }
  } catch (error) {
    log.error("查询支付状态失败", error);
    success.value = false;
    errorMessage.value = (error as { message?: string })?.message || "查询支付状态失败";
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
};

const refreshStatus = () => {
  checkPaymentStatus(false);
};

const goHome = () => {
  router.push("/");
};

const goWallet = () => {
  router.push("/wallet");
};

onMounted(() => {
  log.mounted();

  // 如果URL中有tradeStatus参数，先乐观显示
  if (tradeStatus.value === "TRADE_SUCCESS") {
    success.value = true;
    amount.value = Number(route.query.money) || 0;
  } else if (tradeStatus.value) {
    success.value = false;
    errorMessage.value = tradeStatus.value === "TRADE_CLOSED" ? "交易已关闭" : "支付失败";
    loading.value = false;
    return;
  }

  // 查询后端确认支付状态
  checkPaymentStatus(true);
});
</script>