<template>
  <div class="min-h-[60vh] flex items-center justify-center">
    <div v-if="loading" class="text-center">
      <LoadingSpinner />
      <p class="mt-4 text-gray-500">正在查询支付结果...</p>
    </div>

    <div v-else-if="success" class="text-center">
      <div class="w-20 h-20 mx-auto mb-6 bg-green-100 rounded-full flex items-center justify-center">
        <UIcon name="i-heroicons-check" class="w-10 h-10 text-green-600" />
      </div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">支付成功</h1>
      <p class="text-gray-500 mb-2">充值金额: ¥{{ amount }}</p>
      <p class="text-gray-500 mb-6">当前余额: ¥{{ balance?.toFixed(2) || '0.00' }}</p>
      <UButton color="primary" size="lg" @click="goToWallet">
        返回钱包
      </UButton>
    </div>

    <div v-else class="text-center">
      <div class="w-20 h-20 mx-auto mb-6 bg-red-100 rounded-full flex items-center justify-center">
        <UIcon name="i-heroicons-x-mark" class="w-10 h-10 text-red-600" />
      </div>
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white mb-2">支付失败</h1>
      <p class="text-gray-500 mb-6">{{ errorMessage }}</p>
      <div class="flex gap-4 justify-center">
        <UButton color="neutral" variant="outline" @click="goToWallet">
          返回钱包
        </UButton>
        <UButton color="primary" @click="retryPayment">
          重新充值
        </UButton>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from "../../stores/user";
import { useUserApi } from "../../api/user";
import { useAppToast } from "../../composables/useToast";
import { createPageLogger } from "../../utils/logger";
import LoadingSpinner from "../components/common/LoadingSpinner.vue";

const log = createPageLogger("payment-result");

definePageMeta({
  middleware: ["auth"],
});

const route = useRoute();
const router = useRouter();
const toast = useAppToast();
const userStore = useUserStore();
const userApi = useUserApi();

const loading = ref(true);
const success = ref(false);
const amount = ref<number>(0);
const balance = ref<number | null>(null);
const errorMessage = ref("支付未完成或已取消");

const outTradeNo = computed(() => route.query.outTradeNo as string);
const tradeStatus = computed(() => route.query.tradeStatus as string);

const checkPaymentStatus = async () => {
  if (!outTradeNo.value) {
    log.warn("缺少订单号参数");
    loading.value = false;
    return;
  }

  log.info("查询支付结果", { outTradeNo: outTradeNo.value, tradeStatus: tradeStatus.value });

  try {
    const result = await userApi.getRechargeStatus(outTradeNo.value);
    log.success("查询成功", result);

    if (result.status === 1) {
      success.value = true;
      amount.value = result.amount;
      balance.value = result.balance;
      toast.success("充值成功");
    } else {
      success.value = false;
      errorMessage.value = "支付处理中，请稍后查看";
    }
  } catch (error) {
    log.error("查询支付状态失败", error);
    success.value = false;
    errorMessage.value = (error as { message?: string })?.message || "查询支付状态失败";
  } finally {
    loading.value = false;
  }
};

const goToWallet = () => {
  router.push("/wallet");
};

const retryPayment = () => {
  router.push("/wallet");
};

onMounted(() => {
  log.mounted();
  // 如果URL中有tradeStatus参数，直接判断
  if (tradeStatus.value === "TRADE_SUCCESS") {
    success.value = true;
    amount.value = Number(route.query.money) || 0;
    loading.value = true;
    // 仍然查询后端确认
    checkPaymentStatus();
  } else if (tradeStatus.value) {
    success.value = false;
    errorMessage.value = tradeStatus.value === "TRADE_CLOSED" ? "交易已关闭" : "支付失败";
    loading.value = false;
  } else {
    // 没有tradeStatus参数，查询后端
    checkPaymentStatus();
  }
});
</script>