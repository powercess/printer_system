<template>
  <NuxtLayout name="admin">
    <div class="space-y-6">
      <div class="flex items-center justify-between">
        <h1 class="text-2xl font-bold text-gray-900 dark:text-white">
          系统设置
        </h1>
      </div>

      <!-- Pricing Settings -->
      <UCard>
        <template #header>
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-outline-currency-yen" class="w-5 h-5 text-primary" />
            <h3 class="font-semibold">定价设置</h3>
          </div>
        </template>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <UFormField label="单面打印价格（元/张）">
            <UInput v-model="settings.priceSimplex" type="number" step="0.01" />
          </UFormField>
          <UFormField label="双面打印价格（元/张）">
            <UInput v-model="settings.priceDuplex" type="number" step="0.01" />
          </UFormField>
          <UFormField label="第三方支付手续费率（%）">
            <UInput v-model="settings.paymentFee" type="number" step="0.1" />
          </UFormField>
          <UFormField label="手续费计算方式">
            <USelect v-model="settings.feeMethod" :items="feeMethodOptions" />
          </UFormField>
        </div>

        <template #footer>
          <div class="flex justify-end">
            <UButton color="primary" @click="savePricingSettings">
              保存定价设置
            </UButton>
          </div>
        </template>
      </UCard>

      <!-- Printer Settings -->
      <UCard>
        <template #header>
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-outline-printer" class="w-5 h-5 text-primary" />
            <h3 class="font-semibold">打印机设置</h3>
          </div>
        </template>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <UFormField label="CUPS 服务器地址">
            <UInput v-model="settings.cupsHost" placeholder="localhost" />
          </UFormField>
          <UFormField label="CUPS 端口">
            <UInput v-model="settings.cupsPort" type="number" placeholder="631" />
          </UFormField>
          <UFormField label="默认打印机">
            <USelect v-model="settings.defaultPrinter" :items="printerOptions" placeholder="选择默认打印机" />
          </UFormField>
          <UFormField label="打印超时时间（秒）">
            <UInput v-model="settings.printTimeout" type="number" />
          </UFormField>
        </div>

        <template #footer>
          <div class="flex justify-end">
            <UButton color="primary" @click="savePrinterSettings">
              保存打印机设置
            </UButton>
          </div>
        </template>
      </UCard>

      <!-- Payment Settings -->
      <UCard>
        <template #header>
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-outline-credit-card" class="w-5 h-5 text-primary" />
            <h3 class="font-semibold">支付设置</h3>
          </div>
        </template>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
          <UFormField label="支付网关 PID">
            <UInput v-model="settings.payPid" type="password" placeholder="请输入 PID" />
          </UFormField>
          <UFormField label="支付网关 Key">
            <UInput v-model="settings.payKey" type="password" placeholder="请输入 Key" />
          </UFormField>
          <UFormField label="支付回调地址">
            <UInput v-model="settings.notifyUrl" placeholder="https://your-domain.com/api/payment/notify" />
          </UFormField>
          <UFormField label="前端地址">
            <UInput v-model="settings.frontendUrl" placeholder="https://your-domain.com" />
          </UFormField>
        </div>

        <template #footer>
          <div class="flex justify-end">
            <UButton color="primary" @click="savePaymentSettings">
              保存支付设置
            </UButton>
          </div>
        </template>
      </UCard>

      <!-- System Info -->
      <UCard>
        <template #header>
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-outline-information-circle" class="w-5 h-5 text-primary" />
            <h3 class="font-semibold">系统信息</h3>
          </div>
        </template>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <div>
            <p class="text-gray-500">系统版本</p>
            <p class="font-medium">v1.0.0</p>
          </div>
          <div>
            <p class="text-gray-500">Java 版本</p>
            <p class="font-medium">17+</p>
          </div>
          <div>
            <p class="text-gray-500">数据库</p>
            <p class="font-medium">MySQL 8.0+</p>
          </div>
          <div>
            <p class="text-gray-500">CUPS 版本</p>
            <p class="font-medium">2.4+</p>
          </div>
        </div>
      </UCard>

      <!-- Danger Zone -->
      <UCard>
        <template #header>
          <div class="flex items-center gap-2">
            <UIcon name="i-heroicons-outline-exclamation-triangle" class="w-5 h-5 text-red-500" />
            <h3 class="font-semibold text-red-500">危险操作</h3>
          </div>
        </template>

        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium">清理临时文件</p>
              <p class="text-sm text-gray-500">删除所有过期的预览文件和临时文件</p>
            </div>
            <UButton color="warning" variant="outline" @click="cleanTempFiles">
              清理
            </UButton>
          </div>
          <div class="flex items-center justify-between">
            <div>
              <p class="font-medium">重置所有设置</p>
              <p class="text-sm text-gray-500">恢复所有系统设置为默认值</p>
            </div>
            <UButton color="error" variant="outline" @click="resetSettings">
              重置
            </UButton>
          </div>
        </div>
      </UCard>
    </div>
  </NuxtLayout>
</template>

<script setup lang="ts">
import { useAppToast } from "../../../composables/useToast";

definePageMeta({
  middleware: ["auth", "admin"],
});

const toast = useAppToast();

const settings = reactive({
  // Pricing
  priceSimplex: 0.20,
  priceDuplex: 0.20,
  paymentFee: 2.0,
  feeMethod: "ceiling",

  // Printer
  cupsHost: "localhost",
  cupsPort: 631,
  defaultPrinter: "",
  printTimeout: 300,

  // Payment
  payPid: "",
  payKey: "",
  notifyUrl: "",
  frontendUrl: "",
});

const feeMethodOptions = [
  { value: "ceiling", label: "向上取整" },
  { value: "round", label: "四舍五入" },
  { value: "floor", label: "向下取整" },
];

const printerOptions = ref([
  { value: "printer1", label: "打印机 1" },
  { value: "printer2", label: "打印机 2" },
]);

const savePricingSettings = () => {
  toast.success("定价设置已保存");
};

const savePrinterSettings = () => {
  toast.success("打印机设置已保存");
};

const savePaymentSettings = () => {
  toast.success("支付设置已保存");
};

const cleanTempFiles = () => {
  toast.success("临时文件已清理");
};

const resetSettings = () => {
  toast.success("设置已重置");
};
</script>