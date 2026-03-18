<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 py-12 px-4">
    <div class="max-w-md w-full space-y-8">
      <!-- Header -->
      <div class="text-center">
        <UIcon name="i-heroicons-printer" class="w-12 h-12 mx-auto text-primary mb-4" />
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
          登录
        </h1>
        <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
          登录到自助打印系统
        </p>
      </div>

      <!-- Login Form -->
      <UCard>
        <form @submit.prevent="handleLogin" class="space-y-6">
          <UFormField label="用户名" name="username" required>
            <UInput
              v-model="form.username"
              placeholder="请输入用户名"
              icon="i-heroicons-user"
            />
          </UFormField>

          <UFormField label="密码" name="password" required>
            <UInput
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              icon="i-heroicons-lock-closed"
            />
          </UFormField>

          <UButton
            type="submit"
            color="primary"
            block
            :loading="loading"
          >
            登录
          </UButton>
        </form>

        <div class="mt-6 text-center text-sm text-gray-600 dark:text-gray-400">
          还没有账号？
          <NuxtLink to="/register" class="text-primary hover:underline">
            立即注册
          </NuxtLink>
        </div>
      </UCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from "../../stores/auth";
import { useAppToast } from "../../composables/useToast";

definePageMeta({
  layout: false,
  middleware: ["guest"],
});

const authStore = useAuthStore();
const toast = useAppToast();
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
});

const handleLogin = async () => {
  if (!form.username || !form.password) {
    toast.error("请填写用户名和密码");
    return;
  }

  loading.value = true;

  try {
    const result = await authStore.login(form.username, form.password);

    if (result.success) {
      toast.success("登录成功");
      navigateTo("/");
    } else {
      toast.error(result.message || "登录失败");
    }
  } finally {
    loading.value = false;
  }
};
</script>