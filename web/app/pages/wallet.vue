<template>
  <div class="space-y-6">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
        我的钱包
      </h1>
    </div>

    <!-- Balance Card -->
    <UCard class="bg-gradient-to-r from-primary to-primary-600 text-white">
      <div class="text-center py-6">
        <p class="text-sm opacity-80">账户余额</p>
        <p class="text-4xl font-bold mt-2">¥{{ (balance ?? 0).toFixed(2) }}</p>
      </div>
    </UCard>

    <!-- Recharge Section -->
    <UCard>
      <template #header>
        <h2 class="text-lg font-semibold">充值</h2>
      </template>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
        <UButton
          v-for="amount in rechargeAmounts"
          :key="amount"
          color="neutral"
          variant="outline"
          size="lg"
          :class="{ 'ring-2 ring-primary': selectedAmount === amount }"
          @click="selectedAmount = amount"
        >
          ¥{{ amount }}
        </UButton>
      </div>

      <div class="mt-4">
        <UFormField label="自定义金额">
          <UInput
            v-model.number="customAmount"
            type="number"
            placeholder="输入充值金额"
            min="1"
          />
        </UFormField>
      </div>

      <div class="mt-6">
        <UFormField label="支付方式">
          <URadioGroup v-model="paymentMethod" :items="paymentMethodOptions" />
        </UFormField>
      </div>

      <div class="mt-6 flex justify-end">
        <UButton
          color="primary"
          size="lg"
          :loading="recharging"
          :disabled="!rechargeAmount"
          @click="handleRecharge"
        >
          立即充值 ¥{{ rechargeAmount }}
        </UButton>
      </div>
    </UCard>

    <!-- Transaction History -->
    <UCard>
      <template #header>
        <div class="flex items-center justify-between">
          <h2 class="text-lg font-semibold">交易记录</h2>
          <USelect
            v-model="transactionType"
            :items="transactionTypeOptions"
            placeholder="全部类型"
            class="w-32"
            @update:model-value="fetchTransactions"
          />
        </div>
      </template>

      <div v-if="loadingTransactions" class="flex justify-center py-8">
        <LoadingSpinner />
      </div>

      <div v-else-if="transactions.length === 0" class="text-center py-8 text-gray-500">
        暂无交易记录
      </div>

      <div v-else class="divide-y divide-gray-200 dark:divide-gray-700">
        <div
          v-for="tx in transactions"
          :key="tx.id"
          class="flex items-center justify-between py-4"
        >
          <div class="flex items-center gap-3">
            <div
              class="w-10 h-10 rounded-full flex items-center justify-center"
              :class="getTypeStyle(tx.type).bgClass"
            >
              <UIcon :name="getTypeStyle(tx.type).icon" class="w-5 h-5" />
            </div>
            <div>
              <p class="font-medium">{{ getTypeLabel(tx.type) }}</p>
              <p class="text-sm text-gray-500">{{ formatDate(tx.createdAt) }}</p>
            </div>
          </div>
          <p
            class="text-lg font-bold"
            :class="getTypeStyle(tx.type).textClass"
          >
            {{ getTypeStyle(tx.type).prefix }}¥{{ tx.amount.toFixed(2) }}
          </p>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="mt-6 flex justify-center">
        <Pagination
          v-model:current-page="currentPage"
          :total-pages="totalPages"
          @change="fetchTransactions"
        />
      </div>
    </UCard>
  </div>
</template>

<script setup lang="ts">
import type { Transaction } from "../../types/user";
import { TRANSACTION_TYPE_MAP } from "../../types/user";
import { useUserStore } from "../../stores/user";
import { useAppToast } from "../../composables/useToast";
import { useUserApi } from "../../api/user";
import { createPageLogger } from "../../utils/logger";
import LoadingSpinner from "../components/common/LoadingSpinner.vue";
import Pagination from "../components/common/Pagination.vue";

const log = createPageLogger("wallet");

definePageMeta({
  middleware: ["auth"],
});

const toast = useAppToast();
const userStore = useUserStore();
const userApi = useUserApi();
const balance = computed(() => userStore.balance);

const rechargeAmounts = [10, 20, 50, 100];
const selectedAmount = ref<number | null>(null);
const customAmount = ref<number | null>(null);
const paymentMethod = ref<"alipay" | "wechat">("alipay");
const recharging = ref(false);

const rechargeAmount = computed(() => customAmount.value || selectedAmount.value || 0);

const paymentMethodOptions = [
  { value: "alipay", label: "支付宝" },
  { value: "wechat", label: "微信支付" },
];

const loadingTransactions = ref(false);
const transactions = ref<Transaction[]>([]);
const currentPage = ref(1);
const totalPages = ref(1);
const transactionType = ref<string>("all");
const pageSize = 10;

const transactionTypeOptions = [
  { value: "all", label: "全部" },
  { value: "recharge", label: "充值" },
  { value: "consume", label: "消费" },
  { value: "refund", label: "退款" },
];

const getTypeStyle = (type: number) => {
  const typeStr = TRANSACTION_TYPE_MAP[type] || "consume";
  const styles: Record<string, { bgClass: string; textClass: string; icon: string; prefix: string }> = {
    recharge: { bgClass: "bg-green-100 text-green-600", textClass: "text-green-600", icon: "i-heroicons-plus", prefix: "+" },
    consume: { bgClass: "bg-red-100 text-red-600", textClass: "text-red-600", icon: "i-heroicons-minus", prefix: "-" },
    refund: { bgClass: "bg-blue-100 text-blue-600", textClass: "text-green-600", icon: "i-heroicons-arrow-uturn-left", prefix: "+" },
  };
  return styles[typeStr] || styles.consume;
};

const getTypeLabel = (type: number): string => {
  const labels: Record<string, string> = {
    recharge: "充值",
    consume: "消费",
    refund: "退款",
  };
  return labels[TRANSACTION_TYPE_MAP[type] || "consume"];
};

const formatDate = (dateStr: string) => {
  return new Date(dateStr).toLocaleString("zh-CN");
};

const handleRecharge = async () => {
  log.formSubmit("充值表单", { amount: rechargeAmount.value, paymentMethod: paymentMethod.value });

  if (!rechargeAmount.value || rechargeAmount.value <= 0) {
    log.warn("充值金额无效", { amount: rechargeAmount.value });
    toast.error("请选择或输入充值金额");
    return;
  }

  log.userAction("发起充值", { amount: rechargeAmount.value, paymentMethod: paymentMethod.value });
  recharging.value = true;
  try {
    log.time("充值请求");
    await userApi.recharge({
      amount: rechargeAmount.value,
      payment_method: paymentMethod.value,
    });
    log.timeEnd("充值请求");

    log.success("充值成功", { amount: rechargeAmount.value });
    toast.success("充值成功");
    await userStore.fetchBalance();
    await fetchTransactions();

    // Reset selection
    selectedAmount.value = null;
    customAmount.value = null;
  } catch (error) {
    log.error("充值失败", error);
    toast.error((error as { message?: string })?.message || "充值失败");
  } finally {
    recharging.value = false;
  }
};

const fetchTransactions = async () => {
  log.loadStart("交易记录");
  loadingTransactions.value = true;
  try {
    const result = await userApi.getTransactions({
      page: currentPage.value,
      page_size: pageSize,
      type: transactionType.value === "all" ? undefined : transactionType.value as "recharge" | "consume" | "refund",
    });
    transactions.value = result.items;
    totalPages.value = Math.ceil(result.total / pageSize);
    log.loadSuccess("交易记录", { count: result.items.length, total: result.total });
  } catch (error) {
    log.loadError("交易记录", error);
    toast.error("获取交易记录失败");
  } finally {
    loadingTransactions.value = false;
  }
};

onMounted(() => {
  log.mounted();
  log.debug("初始化钱包页面，获取余额和交易记录");
  userStore.fetchBalance();
  fetchTransactions();
});
</script>