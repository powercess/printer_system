<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900 py-12 px-4">
    <div class="max-w-md w-full space-y-8">
      <!-- Header -->
      <div class="text-center">
        <UIcon name="i-heroicons-printer" class="w-12 h-12 mx-auto text-primary mb-4" />
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">
          注册
        </h1>
        <p class="mt-2 text-sm text-gray-600 dark:text-gray-400">
          创建自助打印系统账号
        </p>
      </div>

      <!-- Register Form -->
      <UCard>
        <form @submit.prevent="handleRegister" class="space-y-6">
          <UFormField label="用户名" name="username" required>
            <UInput
              v-model="form.username"
              placeholder="请输入用户名"
              icon="i-heroicons-user"
            />
          </UFormField>

          <UFormField label="邮箱" name="email" required>
            <UInput
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱"
              icon="i-heroicons-envelope"
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

          <UFormField label="确认密码" name="confirmPassword" required>
            <UInput
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              icon="i-heroicons-lock-closed"
            />
          </UFormField>

          <UButton
            type="submit"
            color="primary"
            block
            :loading="loading"
          >
            注册
          </UButton>
        </form>

        <div class="mt-6 text-center text-sm text-gray-600 dark:text-gray-400">
          已有账号？
          <NuxtLink to="/login" class="text-primary hover:underline">
            立即登录
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
  email: "",
  password: "",
  confirmPassword: "",
});

const handleRegister = async () => {
  if (!form.username || !form.email || !form.password || !form.confirmPassword) {
    toast.error("请填写所有必填项");
    return;
  }

  if (form.password !== form.confirmPassword) {
    toast.error("两次输入的密码不一致");
    return;
  }

  if (form.password.length < 6) {
    toast.error("密码长度至少为6位");
    return;
  }

  loading.value = true;

  try {
    const result = await authStore.register(form.username, form.password, form.email);

    if (result.success) {
      toast.success("注册成功");
      navigateTo("/");
    } else {
      toast.error(result.message || "注册失败");
    }
  } finally {
    loading.value = false;
  }
};
</script>