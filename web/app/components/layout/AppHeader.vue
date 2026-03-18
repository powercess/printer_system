<template>
  <header class="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
    <div class="container mx-auto px-4">
      <div class="flex items-center justify-between h-16">
        <!-- Logo -->
        <NuxtLink to="/" class="flex items-center space-x-2">
          <UIcon name="i-heroicons-printer" class="w-8 h-8 text-primary" />
          <span class="text-xl font-bold text-gray-900 dark:text-white">
            自助打印
          </span>
        </NuxtLink>

        <!-- Navigation -->
        <nav class="hidden md:flex items-center space-x-6">
          <NuxtLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="text-gray-600 dark:text-gray-300 hover:text-primary dark:hover:text-primary transition-colors"
            :class="{ 'text-primary font-medium': isActive(item.to) }"
          >
            {{ item.label }}
          </NuxtLink>
        </nav>

        <!-- User Menu -->
        <div class="flex items-center space-x-4">
          <template v-if="isLoggedIn">
            <div class="flex items-center space-x-3">
              <UBadge color="primary" variant="subtle">
                余额: ¥{{ balance.toFixed(2) }}
              </UBadge>
              <UDropdownMenu :items="userMenuItems" @select="handleMenuSelect">
                <UButton
                  color="neutral"
                  variant="ghost"
                  trailing-icon="i-heroicons-chevron-down"
                >
                  {{ username }}
                </UButton>
              </UDropdownMenu>
            </div>
          </template>
          <template v-else>
            <UButton to="/login" color="primary" variant="soft">
              登录
            </UButton>
            <UButton to="/register" color="primary">
              注册
            </UButton>
          </template>

          <!-- Mobile Menu -->
          <UButton
            class="md:hidden"
            color="neutral"
            variant="ghost"
            icon="i-heroicons-bars-3"
            @click="mobileMenuOpen = !mobileMenuOpen"
          />
        </div>
      </div>

      <!-- Mobile Navigation -->
      <div v-if="mobileMenuOpen" class="md:hidden py-4 border-t border-gray-200 dark:border-gray-700">
        <nav class="flex flex-col space-y-2">
          <NuxtLink
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="px-3 py-2 rounded-md text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700"
            @click="mobileMenuOpen = false"
          >
            {{ item.label }}
          </NuxtLink>
        </nav>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useAuthStore } from "../../../stores/auth";
import { useUserStore } from "../../../stores/user";

const route = useRoute();
const authStore = useAuthStore();
const userStore = useUserStore();
const mobileMenuOpen = ref(false);

const isLoggedIn = computed(() => authStore.isLoggedIn);
const username = computed(() => userStore.username);
const balance = computed(() => userStore.balance);

const navItems = computed(() => {
  const items = [
    { to: "/", label: "打印" },
    { to: "/files", label: "文件管理" },
    { to: "/orders", label: "订单记录" },
    { to: "/wallet", label: "钱包" },
    { to: "/community", label: "社区" },
  ];

  if (userStore.isAdmin) {
    items.push({ to: "/admin", label: "管理后台" });
  }

  return items;
});

const userMenuItems = [
  [
    {
      label: "个人中心",
      icon: "i-heroicons-user",
    },
  ],
  [
    {
      label: "退出登录",
      icon: "i-heroicons-arrow-right-on-rectangle",
    },
  ],
];

const handleMenuSelect = (e: Event, item: { label: string }) => {
  // Use nextTick to allow dropdown to close before navigation
  nextTick(() => {
    if (item.label === "个人中心") {
      navigateTo("/wallet");
    } else if (item.label === "退出登录") {
      authStore.logout();
    }
  });
};

const isActive = (path: string) => {
  if (path === "/") {
    return route.path === "/";
  }
  return route.path.startsWith(path);
};
</script>